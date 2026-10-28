package dev.viruzia.combatreanimated.mixin;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.viruzia.combatreanimated.AttackAnimatable;
import dev.viruzia.combatreanimated.AttackAnimationController;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements AttackAnimatable {
    @Unique
    private AttackAnimationController combatreanimated$controller;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void combatreanimated$registerLayer(ClientLevel level, GameProfile profile, CallbackInfo ci) {
        combatreanimated$controller = new AttackAnimationController((AbstractClientPlayer) (Object) this);
        ((IAnimatedPlayer) this).getAnimationStack()
                .addAnimLayer(AttackAnimationController.LAYER_PRIORITY, combatreanimated$controller.layer());
    }

    @Override
    public void combatreanimated$onSwingStart(InteractionHand hand) {
        combatreanimated$controller.onSwingStart((AbstractClientPlayer) (Object) this, hand);
    }

    @Override
    public AttackAnimationController combatreanimated$attackController() {
        return combatreanimated$controller;
    }
}
