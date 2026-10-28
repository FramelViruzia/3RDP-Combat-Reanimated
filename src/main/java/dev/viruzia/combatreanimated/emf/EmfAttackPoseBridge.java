package dev.viruzia.combatreanimated.emf;

import dev.viruzia.combatreanimated.AttackAnimatable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hands the attack pose from PlayerModel.setupAnim (where playerAnimator applies it) to the EMF
 * hook that runs right after EMF animates the same model for the same render call.
 * Render thread only.
 */
public final class EmfAttackPoseBridge {
    private static final Map<UUID, CapturedAttackPose> PENDING = new HashMap<>();

    private EmfAttackPoseBridge() {}

    public static void capture(PlayerModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!(entity instanceof AttackAnimatable animatable)) {
            return;
        }
        float weight = animatable.combatreanimated$attackController().weight(ageInTicks);
        if (weight <= 0F) {
            PENDING.remove(entity.getUUID());
            return;
        }
        PENDING.put(entity.getUUID(), CapturedAttackPose.capture(model, weight));
    }

    /** Removes the pose so each capture is applied at most once. */
    @Nullable
    public static CapturedAttackPose take(UUID uuid) {
        return PENDING.remove(uuid);
    }
}
