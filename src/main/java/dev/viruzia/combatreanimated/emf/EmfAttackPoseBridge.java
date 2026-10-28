package dev.viruzia.combatreanimated.emf;

import dev.viruzia.combatreanimated.AttackAnimationController;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hands the attack pose from PlayerModel.setupAnim (where Player Animation Library applies it) to the
 * EMF hook that runs right after EMF animates the same model for the same render call.
 * Render thread only.
 */
public final class EmfAttackPoseBridge {
    private static final Map<UUID, CapturedAttackPose> PENDING = new HashMap<>();

    private EmfAttackPoseBridge() {}

    public static void capture(PlayerModel<?> model, Entity entity, float ageInTicks) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        AttackAnimationController controller = AttackAnimationController.of(player);
        float weight = controller == null ? 0F : controller.weight(ageInTicks);
        if (weight <= 0F) {
            PENDING.remove(player.getUUID());
            return;
        }
        PENDING.put(player.getUUID(), CapturedAttackPose.capture(model, weight));
    }

    /** Removes the pose so each capture is applied at most once. */
    @Nullable
    public static CapturedAttackPose take(UUID uuid) {
        return PENDING.remove(uuid);
    }
}
