package fuzs.fluidcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fuzs.fluidcombat.helper.BlockStanceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void tryBlockWithWeapon(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (BlockStanceHelper.canUseGuardStance(player)) {
            player.startUsingItem(hand); // so it continously uses the item (like a shield)
            BlockStanceHelper.startGuarding(player);
            cir.setReturnValue(InteractionResultHolder.consume(player.getItemInHand(hand)));
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void onGetUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        Player player = Minecraft.getInstance().player;

        // Quick null & context sanity check
        if (player != null && player.getMainHandItem() == stack) {
            if (BlockStanceHelper.canUseGuardStance(player)) {
                cir.setReturnValue(UseAnim.BLOCK);
            }
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void onReleaseUseItem(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        if (entity instanceof Player player) {
            BlockStanceHelper.stopGuarding(player);
        }
    }
}