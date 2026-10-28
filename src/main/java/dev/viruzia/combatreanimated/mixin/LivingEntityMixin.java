package dev.viruzia.combatreanimated.mixin;

import dev.viruzia.combatreanimated.AttackAnimationController;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the vanilla arm swing. On the client this runs for the local player when they attack
 * (LocalPlayer.swing) and for other players when the server's swing packet arrives
 * (ClientPacketListener.handleAnimate), so everyone sees the animation without a custom packet.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("RETURN"))
    private void combatreanimated$onSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Vanilla only resets swingTime to -1 when a new swing actually starts,
        // so repeated calls while mining don't restart the animation every tick
        if (self.level().isClientSide && self.swingTime == -1 && self instanceof AbstractClientPlayer player) {
            AttackAnimationController controller = AttackAnimationController.of(player);
            if (controller != null) {
                controller.onSwingStart(hand);
            }
        }
    }
}
