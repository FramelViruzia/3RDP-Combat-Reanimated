package dev.viruzia.combatreanimated;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.math.Vec3f;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Per-player attack animation layer (one per player, created by Player Animation Library's factory).
 * Purely visual: it reads the vanilla swing and attack speed, and never changes attacks, cooldowns,
 * damage or hit detection.
 *
 * Timing: Better Combat animations wind up from tick 0 to {@code beginTick} and strike right after it,
 * because Better Combat deals damage late. Vanilla deals damage on click, so playback starts at the
 * wind-up pose, plays the strike in {@link #STRIKE_REAL_TICKS}, and stretches the rest of the animation
 * over the remaining attack cooldown.
 */
public class AttackAnimationController extends PlayerAnimationController {
    public static final ResourceLocation LAYER_ID = CombatReanimated.id("attack");
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

    private final PhasedSpeedModifier speed = new PhasedSpeedModifier();
    private final MirrorModifier mirror = new MirrorModifier();

    private int comboIndex = 0;
    private int lastSwingTick = Integer.MIN_VALUE / 2;
    private float startAge = Float.NEGATIVE_INFINITY;
    private float durationTicks = 0;

    public AttackAnimationController(AbstractClientPlayer player) {
        // Only plays animations we trigger; nothing loops on its own
        super(player, (controller, state, animationSetter) -> PlayState.STOP);
        mirror.enabled = false;
        addModifier(mirror, 0);
        addModifier(speed, 0);
        addModifierLast(new AdjustmentModifier(this::lookAdjustment));
        // NONE keeps the vanilla first-person hand; animations show in third person and to other players
        setFirstPersonMode(FirstPersonMode.NONE);
        setPostAnimationSetupConsumer(bones -> {
            var torso = bones.apply("torso");
            if (torso != null) {
                torso.setEnabled(true);
            }
            // Head keeps following the camera
            var head = bones.apply("head");
            if (head != null) {
                head.rotXEnabled = false;
            }
        });
    }

    @Nullable
    public static AttackAnimationController of(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID) instanceof AttackAnimationController controller
                ? controller
                : null;
    }

    public void onSwingStart(InteractionHand hand) {
        AbstractClientPlayer player = getPlayer();
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

        Animation animation = PlayerAnimResources.getAnimation(id);
        if (animation == null) {
            CombatReanimated.LOGGER.warn("Missing player animation {}", id);
            return;
        }

        // For Player Animator format files, length() is the stopTick
        float stop = animation.length();
        float begin = tickData(animation, ExtraAnimationData.BEGIN_TICK_KEY, 0F);
        float strikeEnd = Math.min(begin + STRIKE_ANIMATION_TICKS, stop);
        float strikeReal = Math.min(STRIKE_REAL_TICKS, cooldown * 0.5F);
        float remainingReal = Math.max(cooldown - strikeReal, 1F);
        speed.configure(strikeReal,
                (strikeEnd - begin) / strikeReal,
                (stop - strikeEnd) / remainingReal);

        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        mirror.enabled = (hand == InteractionHand.OFF_HAND) != leftHanded;

        // Fade from the current pose, then restart the same animation from the wind-up pose
        replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(FADE_IN_TICKS, EasingType.EASE_IN_OUT_SINE), animation, true);
        triggerAnimation(animation, begin);

        startAge = player.tickCount;
        durationTicks = cooldown;
    }

    /**
     * How strongly the attack pose should override the resource pack's animation at this moment, 0 to 1.
     * {@code ageInTicks} is the player's tick count plus partial tick, as passed to setupAnim.
     */
    public float weight(float ageInTicks) {
        float t = ageInTicks - startAge;
        if (t < 0 || t >= durationTicks || !isActive()) {
            return 0F;
        }
        float in = Mth.clamp(t / FADE_IN_TICKS, 0F, 1F);
        float out = Mth.clamp((durationTicks - t) / BLEND_OUT_TICKS, 0F, 1F);
        return Math.min(in, out);
    }

    private static float tickData(Animation animation, String key, float fallback) {
        return animation.data().getRaw(key) instanceof Number number ? number.floatValue() : fallback;
    }

    /** Leans the torso and arms with the camera pitch so swings follow where the player looks. */
    private Optional<AdjustmentModifier.PartModifier> lookAdjustment(String part) {
        if (FirstPersonMode.isFirstPersonPass()) {
            return Optional.empty();
        }
        float pitch = (float) Math.toRadians(getPlayer().getXRot());
        return switch (part) {
            // Same factors as Better Combat 2.x applied to its torso; arms match in both libraries
            case "torso" -> Optional.of(rotateX(-pitch * 0.75F));
            case "right_arm", "left_arm" -> Optional.of(rotateX(pitch * 0.25F));
            default -> Optional.empty();
        };
    }

    private static AdjustmentModifier.PartModifier rotateX(float angle) {
        return new AdjustmentModifier.PartModifier(new Vec3f(angle, 0, 0), Vec3f.ZERO);
    }
}
