package fuzs.fluidcombat.helper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class BlockStanceHelper {
    private static final Set<UUID> GUARDING_PLAYERS = ConcurrentHashMap.newKeySet();

    // should DRY this but it doesnt work so fuck it
    public static boolean canUseGuardStance(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) return false; // we have no item, cant block
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.getItem().getUseAnimation(off) == UseAnim.BLOCK) return false; // offhand is a shield, cant block with weapon

        if (!main.has(DataComponents.ATTRIBUTE_MODIFIERS)) return false;

        var modifiers = main.get(DataComponents.ATTRIBUTE_MODIFIERS);
        boolean isWeapon = modifiers.modifiers().stream()
            .anyMatch(attr -> attr.attribute() == Attributes.ATTACK_DAMAGE);

        // Instead of calling getUseAnimation(), check if the item *does something* when used.
        boolean hasRightClickUse = main.getItem().getClass().getSimpleName().contains("Bow")
            || main.getItem().getUseDuration(main, player) > 0;

        return isWeapon && !hasRightClickUse;
    }

    // overload for specific itemstack
    public static boolean canUseGuardStance(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem().getUseAnimation(stack) == UseAnim.BLOCK) return false;
        if (!stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) return false;

        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        boolean isWeapon = modifiers.modifiers().stream()
            .anyMatch(attr -> attr.attribute() == Attributes.ATTACK_DAMAGE);

        boolean hasRightClickUse = stack.getItem().getClass().getSimpleName().contains("Bow")
            || stack.getItem().getUseDuration(stack, player) > 0;

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
            if (hurtItem) weapon.hurtAndBreak(5, player, player.getEquipmentSlotForItem(weapon));
            player.stopUsingItem();
            BlockStanceHelper.stopGuarding(player); // maybe break stance too
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