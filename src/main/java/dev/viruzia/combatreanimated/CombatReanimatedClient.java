package dev.viruzia.combatreanimated;

import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import dev.viruzia.combatreanimated.emf.EmfAttackPoseHook;
import net.fabricmc.api.ClientModInitializer;
import traben.entity_model_features.EMFAnimationApi;

public class CombatReanimatedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                AttackAnimationController.LAYER_ID,
                AttackAnimationController.LAYER_PRIORITY,
                AttackAnimationController::new);

        if (!EMFAnimationApi.registerAnimationHook(new EmfAttackPoseHook())) {
            CombatReanimated.LOGGER.warn("Could not register the EMF animation hook; attack poses may be overwritten by EMF resource packs");
        }
    }
}
