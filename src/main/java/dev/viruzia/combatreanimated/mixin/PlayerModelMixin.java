package dev.viruzia.combatreanimated.mixin;

import dev.viruzia.combatreanimated.emf.EmfAttackPoseBridge;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Higher priority so this runs after playerAnimator has applied its pose at the same point. */
@Mixin(value = PlayerModel.class, priority = 1500)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void combatreanimated$captureAttackPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                     float ageInTicks, float netHeadYaw, float headPitch,
                                                     CallbackInfo ci) {
        EmfAttackPoseBridge.capture((PlayerModel<?>) (Object) this, entity, ageInTicks);
    }
}
