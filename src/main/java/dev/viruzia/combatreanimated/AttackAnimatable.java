package dev.viruzia.combatreanimated;

import net.minecraft.world.InteractionHand;

/** Implemented on AbstractClientPlayer by {@link dev.viruzia.combatreanimated.mixin.AbstractClientPlayerMixin}. */
public interface AttackAnimatable {
    void combatreanimated$onSwingStart(InteractionHand hand);

    AttackAnimationController combatreanimated$attackController();
}
