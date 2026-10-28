package dev.viruzia.combatreanimated;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;

/**
 * Plays the start of an animation at one speed and the rest at another,
 * switching after a number of real (game) ticks.
 */
public class PhasedSpeedModifier extends SpeedModifier {
    private float switchAfterTicks = 0;
    private float firstSpeed = 1;
    private float secondSpeed = 1;
    private int elapsedTicks = 0;

    public PhasedSpeedModifier() {
        super(1F);
    }

    public void configure(float switchAfterTicks, float firstSpeed, float secondSpeed) {
        this.switchAfterTicks = switchAfterTicks;
        this.firstSpeed = firstSpeed;
        this.secondSpeed = secondSpeed;
        this.elapsedTicks = 0;
        this.speed = firstSpeed;
    }

    private float speedAt(float time) {
        return time < switchAfterTicks ? firstSpeed : secondSpeed;
    }

    @Override
    public void tick(AnimationData state) {
        this.speed = speedAt(elapsedTicks);
        super.tick(state);
        elapsedTicks++;
    }

    @Override
    public void setupAnim(AnimationData state) {
        // Read before super, which rewrites the partial tick for the layers below
        this.speed = speedAt(elapsedTicks + state.getPartialTick());
        super.setupAnim(state);
    }
}
