package dev.viruzia.combatreanimated.emf;

import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;

/**
 * Runs after EMF finishes animating a model (EMF's public animation hook API) and puts our attack
 * pose back on the arms and body. Head, legs and everything else stay as the resource pack animated them.
 */
public class EmfAttackPoseHook extends EMFAnimationApi.EMFAnimationHook {
    @Override
    public void onAnimationEnd(AnimationContext context, boolean wasCancelledByHook) {
        EMFEntityRenderState state = context.activeState();
        if (state == null) {
            return;
        }
        CapturedAttackPose pose = EmfAttackPoseBridge.take(state.uuid());
        // First-person hands stay vanilla / resource pack
        if (pose == null || state.isFirstPersonHand()) {
            return;
        }
        pose.applyTo(context.animatingModelRoot().getAllVanillaPartsByNameEMF());
    }
}
