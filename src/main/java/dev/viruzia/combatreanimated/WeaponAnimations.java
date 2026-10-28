package dev.viruzia.combatreanimated;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Which attack animations each weapon cycles through, one per swing.
 * Combos follow Better Combat's presets for the same vanilla weapons.
 * Files live in assets/combatreanimated/player_animations/.
 */
public final class WeaponAnimations {
    private static final ResourceLocation SLASH_RIGHT = CombatReanimated.id("one_handed_slash_horizontal_right");
    private static final ResourceLocation SLASH_LEFT = CombatReanimated.id("one_handed_slash_horizontal_left");
    private static final ResourceLocation STAB = CombatReanimated.id("one_handed_stab");
    private static final ResourceLocation SLAM = CombatReanimated.id("one_handed_slam_instant");

    private static final List<ResourceLocation> SWORD = List.of(SLASH_RIGHT, SLASH_LEFT, STAB);
    private static final List<ResourceLocation> AXE = List.of(SLASH_RIGHT, SLASH_LEFT);
    private static final List<ResourceLocation> MACE = List.of(SLAM);
    private static final List<ResourceLocation> TRIDENT = List.of(STAB);

    /** Convention tag many modded melee weapons use. */
    private static final TagKey<Item> MELEE_WEAPONS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/melee_weapon"));

    private WeaponAnimations() {}

    /** Empty when the item should keep the vanilla swing. */
    public static List<ResourceLocation> comboFor(ItemStack stack) {
        if (stack.is(Items.MACE)) {
            return MACE;
        }
        if (stack.is(Items.TRIDENT)) {
            return TRIDENT;
        }
        if (stack.is(ItemTags.AXES)) {
            return AXE;
        }
        if (stack.is(ItemTags.SWORDS) || stack.is(MELEE_WEAPONS)) {
            return SWORD;
        }
        return List.of();
    }
}
