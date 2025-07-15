package fuzs.fluidcombat.handler;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.client.helper.UseItemFabricClientHelper;
import fuzs.fluidcombat.config.ServerConfig;
import fuzs.fluidcombat.helper.GuardStanceHelper;
import fuzs.fluidcombat.helper.UseItemFabricHelper;
import fuzs.puzzleslib.api.core.v1.ModLoaderEnvironment;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import fuzs.puzzleslib.api.event.v1.core.EventResultHolder;
import fuzs.puzzleslib.api.event.v1.data.MutableFloat;
import fuzs.puzzleslib.api.event.v1.data.MutableInt;
import fuzs.fluidcombat.mixin.accessor.PlayerAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class CombatTestHandler {
    

    public static void onStartPlayerTick(Player player) {
        // switching items no longer triggers the attack cooldown
        ItemStack stack = player.getMainHandItem();
        if (!ItemStack.matches(((PlayerAccessor) player).goldenagecombat$getLastItemInMainHand(), stack)) {
            ((PlayerAccessor) player).goldenagecombat$setLastItemInMainHand(stack.copy());
            // resets guard stance when switching items
            if (GuardStanceHelper.isGuarding(player)) {
                GuardStanceHelper.stopGuarding(player);
                player.stopUsingItem();
            }
        }
    }

    public static EventResult onUseItemStart(LivingEntity entity, ItemStack stack, MutableInt remainingUseDuration) {
        if (FluidCombat.CONFIG.get(ServerConfig.class).removeShieldDelay && stack.getUseAnimation() == UseAnim.BLOCK) {
            // remove shield activation delay
            remainingUseDuration.accept(stack.getUseDuration() - 5);
        }
        if (FluidCombat.CONFIG.get(ServerConfig.class).fastDrinking && stack.getUseAnimation() == UseAnim.DRINK) {
            remainingUseDuration.accept(20);
        }
        return EventResult.PASS;
    }

    public static EventResultHolder<InteractionResultHolder<ItemStack>> onUseItem(Player player, Level level, InteractionHand hand) {
        if (!FluidCombat.CONFIG.get(ServerConfig.class).throwablesDelay) return EventResultHolder.pass();
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.getItem() instanceof SnowballItem || itemInHand.getItem() instanceof EggItem) {
            // add delay after using an item
            player.getCooldowns().addCooldown(itemInHand.getItem(), 4);
            // the callback runs before cooldowns on Fabric, so we need to perform the interaction ourselves and cancel the callback
            if (!ModLoaderEnvironment.INSTANCE.getModLoader().isForge()) {
                InteractionResult result;
                if (level.isClientSide) {
                    result = UseItemFabricClientHelper.useItem(player, hand);
                } else {
                    result = UseItemFabricHelper.useItem((ServerPlayer) player, level, itemInHand, hand);
                }
                return EventResultHolder.interrupt(InteractionResultHolder.success(itemInHand));
            }
        }
        return EventResultHolder.pass();
    }

     public static EventResult onLivingHurt(LivingEntity entity, DamageSource source, MutableFloat amount) {
        if (FluidCombat.CONFIG.get(ServerConfig.class).eatingInterruption) {
            UseAnim useAction = entity.getUseItem().getUseAnimation();
            if (useAction == UseAnim.EAT || useAction == UseAnim.DRINK) {
                entity.stopUsingItem();
            }
        }
        if (FluidCombat.CONFIG.get(ServerConfig.class).noProjectileImmunity) {
            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                // immediately reset damage immunity after being hit by any projectile, fixes multishot
                entity.invulnerableTime = 0;
            }
        }
        return EventResult.PASS;
    }
}
