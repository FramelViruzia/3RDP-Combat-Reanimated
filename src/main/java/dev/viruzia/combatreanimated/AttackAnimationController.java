package dev.viruzia.combatreanimated;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;
import java.util.Optional;

/**
 * Per-player attack animation state. Purely visual: it reads the vanilla swing and attack speed,
 * and never changes attacks, cooldowns, damage or hit detection.
 *
 * Timing: Better Combat animations wind up from tick 0 to {@code beginTick} and strike right after it,
 * because Better Combat deals damage late. Vanilla deals damage on click, so playback starts at the
 * wind-up pose, plays the strike in {@link #STRIKE_REAL_TICKS}, and stretches the rest of the animation
 * over the remaining attack cooldown.
 */
public class AttackAnimationController {
    public static final int LAYER_PRIORITY = 2000;

    /** Animation ticks after {@code beginTick} that make up the strike itself. */
    private static final float STRIKE_ANIMATION_TICKS = 3F;
    /** Real ticks the strike takes on screen. */
    private static final float STRIKE_REAL_TICKS = 2F;
    private static final int FADE_IN_TICKS = 1;
    /** Real ticks at the end where the pose hands back to the resource pack animation. */
    private static final float BLEND_OUT_TICKS = 3F;
    private static final float MIN_COOLDOWN_TICKS = 4F;
    /** Combo restarts after idling this many attack cooldowns. */
    private static final float COMBO_RESET_COOLDOWNS = 3F;

    private final ModifierLayer<IAnimation> layer = new ModifierLayer<>(null);
    private final PhasedSpeedModifier speed = new PhasedSpeedModifier();
    private final MirrorModifier mirror = new MirrorModifier();

    private int comboIndex = 0;
    private int lastSwingTick = Integer.MIN_VALUE / 2;
    private float startAge = Float.NEGATIVE_INFINITY;
    private float durationTicks = 0;

    public AttackAnimationController(AbstractClientPlayer player) {
        mirror.setEnabled(false);
        // Each is inserted at the front, so the final order is mirror -> speed -> look adjustment
        layer.addModifier(new AdjustmentModifier(part -> lookAdjustment(player, part)), 0);
        layer.addModifier(speed, 0);
        layer.addModifier(mirror, 0);
    }

    public ModifierLayer<IAnimation> layer() {
        return layer;
    }

    public void onSwingStart(AbstractClientPlayer player, InteractionHand hand) {
        // swing() can run more than once in a tick
        if (player.tickCount == lastSwingTick) {
            return;
        }
        List<ResourceLocation> combo = WeaponAnimations.comboFor(player.getItemInHand(hand));
        if (combo.isEmpty()) {
            return;
        }

        // Attack cooldown length in ticks, from the vanilla attack speed attribute
        float cooldown = Math.max(player.getCurrentItemAttackStrengthDelay(), MIN_COOLDOWN_TICKS);
        if (player.tickCount - lastSwingTick > cooldown * COMBO_RESET_COOLDOWNS) {
            comboIndex = 0;
        }
        lastSwingTick = player.tickCount;

        ResourceLocation id = combo.get(comboIndex % combo.size());
        comboIndex = (comboIndex + 1) % combo.size();

        KeyframeAnimation animation = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(id);
        if (animation == null) {
            CombatReanimated.LOGGER.warn("Missing player animation {}", id);
            return;
        }

        KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
        // Head keeps following the camera
        copy.head.pitch.setEnabled(false);
        copy.torso.fullyEnablePart(true);

        float begin = animation.beginTick;
        float strikeEnd = Math.min(begin + STRIKE_ANIMATION_TICKS, animation.stopTick);
        float strikeReal = Math.min(STRIKE_REAL_TICKS, cooldown * 0.5F);
        float remainingReal = Math.max(cooldown - strikeReal, 1F);
        speed.configure(strikeReal,
                (strikeEnd - begin) / strikeReal,
                (animation.stopTick - strikeEnd) / remainingReal);

        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        mirror.setEnabled((hand == InteractionHand.OFF_HAND) != leftHanded);

        KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(copy.build(), animation.beginTick);
        // NONE keeps the vanilla first-person hand; animations show in third person and to other players
        animationPlayer.setFirstPersonMode(FirstPersonMode.NONE);
        layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(FADE_IN_TICKS, Ease.INOUTSINE), animationPlayer);

        startAge = player.tickCount;
        durationTicks = cooldown;
    }

    /**
     * How strongly the attack pose should override the resource pack's animation at this moment, 0 to 1.
     * {@code ageInTicks} is the player's tick count plus partial tick, as passed to setupAnim.
     */
    public float weight(float ageInTicks) {
        float t = ageInTicks - startAge;
        if (t < 0 || t >= durationTicks || !layer.isActive()) {
            return 0F;
        }
        float in = Mth.clamp(t / FADE_IN_TICKS, 0F, 1F);
        float out = Mth.clamp((durationTicks - t) / BLEND_OUT_TICKS, 0F, 1F);
        return Math.min(in, out);
    }

    /** Leans the body and arms with the camera pitch so swings follow where the player looks. */
    private static Optional<AdjustmentModifier.PartModifier> lookAdjustment(AbstractClientPlayer player, String part) {
        if (FirstPersonMode.isFirstPersonPass()) {
            return Optional.empty();
        }
        float pitch = (float) Math.toRadians(player.getXRot());
        return switch (part) {
            case "body" -> Optional.of(rotateX(-pitch * 0.75F));
            case "rightArm", "leftArm" -> Optional.of(rotateX(pitch * 0.25F));
            default -> Optional.empty();
        };
    }

    private static AdjustmentModifier.PartModifier rotateX(float angle) {
        return new AdjustmentModifier.PartModifier(new Vec3f(angle, 0, 0), new Vec3f(0, 0, 0));
    }
}
