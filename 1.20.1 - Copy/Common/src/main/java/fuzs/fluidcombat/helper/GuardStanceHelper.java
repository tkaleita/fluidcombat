package fuzs.fluidcombat.helper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class GuardStanceHelper {
    private static final Set<UUID> GUARDING_PLAYERS = ConcurrentHashMap.newKeySet();

    // should DRY this but it causes recursion so fuck it
    public static boolean canUseGuardStance(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) return false; // we have no item, can't block
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.getItem().getUseAnimation(off) == UseAnim.BLOCK) return false; // offhand is a shield, can't block with weapon

        // Get attribute modifiers from main hand slot
        Multimap<Attribute, AttributeModifier> modifiers = main.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.isEmpty()) return false;

        boolean isWeapon = modifiers.containsKey(Attributes.ATTACK_DAMAGE);

        // Instead of calling getUseAnimation(), check if the item *does something* when used.
        boolean hasRightClickUse = main.getItem().getClass().getSimpleName().contains("Bow")
            || main.getItem().getUseDuration(main) > 0;

        return isWeapon && !hasRightClickUse;
    }


    // overload for specific itemstack
    public static boolean canUseGuardStance(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem().getUseAnimation(stack) == UseAnim.BLOCK) return false;

        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.isEmpty()) return false;

        boolean isWeapon = modifiers.containsKey(Attributes.ATTACK_DAMAGE);

        boolean hasRightClickUse = stack.getItem().getClass().getSimpleName().contains("Bow")
            || stack.getItem().getUseDuration(stack) > 0;

        return isWeapon && !hasRightClickUse;
    }

    public static void disableAllGuardStances(Player player, int cooldown) {
        for (ItemStack stack : player.getInventory().items) {
            //System.out.println(stack);
            if (!stack.isEmpty() && canUseGuardStance(player, stack)) {
                player.getCooldowns().addCooldown(stack.getItem(), cooldown);
            }
        }

        player.stopUsingItem();
        stopGuarding(player);
    }


    public static void disableGuardStance(Player player, int cooldown, boolean hurtItem) {
        ItemStack weapon = player.getMainHandItem();
            if (!weapon.isEmpty()) {
                player.getCooldowns().addCooldown(weapon.getItem(), cooldown);
            }
            if (hurtItem) weapon.hurtAndBreak(5, player, (p) -> { p.broadcastBreakEvent(player.getUsedItemHand()); } );
            player.stopUsingItem();
            GuardStanceHelper.stopGuarding(player); // maybe break stance too
    }

    public static void startGuarding(Player player) {
        GUARDING_PLAYERS.add(player.getUUID());
    }

    public static void stopGuarding(Player player) {
        GUARDING_PLAYERS.remove(player.getUUID());
    }

    public static boolean isGuarding(Player player) {
        return GUARDING_PLAYERS.contains(player.getUUID());
    }
}