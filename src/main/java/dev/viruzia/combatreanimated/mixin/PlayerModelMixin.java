package dev.viruzia.combatreanimated.mixin;

import dev.viruzia.combatreanimated.emf.EmfAttackPoseBridge;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player Animation Library applies its pose at the RETURN of this (Entity) overload with priority 2001,
 * so this runs at the same point with a higher priority to read the finished pose.
 */
@Mixin(value = PlayerModel.class, priority = 2100)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("RETURN"))
    private void combatreanimated$captureAttackPose(Entity entity, float limbSwing, float limbSwingAmount,
                                                     float ageInTicks, float netHeadYaw, float headPitch,
                                                     CallbackInfo ci) {
        EmfAttackPoseBridge.capture((PlayerModel<?>) (Object) this, entity, ageInTicks);
    }
}
