package dev.viruzia.combatreanimated.emf;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * The body and arm transforms our attack animation produced in PlayerModel.setupAnim,
 * re-applied on top of the resource pack's EMF animation.
 *
 * Arms keep the exact rotation of the attack; their position and the whole body move by the
 * animation's offset from the vanilla rest pose, so they stay attached to wherever the resource
 * pack placed the body (crouching, bobbing, leaning) instead of snapping to vanilla positions.
 */
public record CapturedAttackPose(PartTransform body, PartTransform rightArm, PartTransform leftArm, float weight) {
    // Vanilla HumanoidModel rest pose (standing)
    private static final PartTransform BODY_REST = new PartTransform(0, 0, 0, 0, 0, 0);
    private static final PartTransform RIGHT_ARM_REST = new PartTransform(-5, 2, 0, 0, 0, 0);
    private static final PartTransform LEFT_ARM_REST = new PartTransform(5, 2, 0, 0, 0, 0);

    public static CapturedAttackPose capture(PlayerModel<?> model, float weight) {
        return new CapturedAttackPose(
                PartTransform.of(model.body),
                PartTransform.of(model.rightArm),
                PartTransform.of(model.leftArm),
                weight);
    }

    /** {@code parts} is EMF's map of vanilla part names to the parts it just animated. */
    public void applyTo(Map<String, ? extends ModelPart> parts) {
        ModelPart bodyPart = parts.get("body");
        ModelPart rightArmPart = parts.get("right_arm");
        ModelPart leftArmPart = parts.get("left_arm");

        if (bodyPart != null) {
            addOffset(bodyPart, body, BODY_REST, true);
            copyIfPresent(parts, "jacket", bodyPart);
        }
        if (rightArmPart != null) {
            applyArm(rightArmPart, rightArm, RIGHT_ARM_REST);
            copyIfPresent(parts, "right_sleeve", rightArmPart);
        }
        if (leftArmPart != null) {
            applyArm(leftArmPart, leftArm, LEFT_ARM_REST);
            copyIfPresent(parts, "left_sleeve", leftArmPart);
        }
    }

    private void applyArm(ModelPart part, PartTransform attack, PartTransform rest) {
        addOffset(part, attack, rest, false);
        part.xRot = Mth.lerp(weight, part.xRot, attack.xRot());
        part.yRot = Mth.lerp(weight, part.yRot, attack.yRot());
        part.zRot = Mth.lerp(weight, part.zRot, attack.zRot());
    }

    private void addOffset(ModelPart part, PartTransform attack, PartTransform rest, boolean rotation) {
        part.x += weight * (attack.x() - rest.x());
        part.y += weight * (attack.y() - rest.y());
        part.z += weight * (attack.z() - rest.z());
        if (rotation) {
            part.xRot += weight * (attack.xRot() - rest.xRot());
            part.yRot += weight * (attack.yRot() - rest.yRot());
            part.zRot += weight * (attack.zRot() - rest.zRot());
        }
    }

    private static void copyIfPresent(Map<String, ? extends ModelPart> parts, String name, ModelPart source) {
        ModelPart target = parts.get(name);
        if (target != null) {
            target.copyFrom(source);
        }
    }

    public record PartTransform(float x, float y, float z, float xRot, float yRot, float zRot) {
        static PartTransform of(ModelPart part) {
            return new PartTransform(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }
    }
}
