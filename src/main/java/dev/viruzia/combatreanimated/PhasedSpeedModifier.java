package dev.viruzia.combatreanimated;

import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;

/**
 * Plays the start of an animation at one speed and the rest at another,
 * switching after a number of real (game) ticks.
 */
public class PhasedSpeedModifier extends SpeedModifier {
    private float switchAfterTicks = 0;
    private float firstSpeed = 1;
    private float secondSpeed = 1;
    private int elapsedTicks = 0;

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
    public void tick() {
        this.speed = speedAt(elapsedTicks);
        super.tick();
        elapsedTicks++;
    }

    @Override
    public void setupAnim(float tickDelta) {
        this.speed = speedAt(elapsedTicks + tickDelta);
        super.setupAnim(tickDelta);
    }
}
