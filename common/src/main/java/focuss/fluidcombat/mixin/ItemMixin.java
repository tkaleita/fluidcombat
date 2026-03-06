package focuss.fluidcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import focuss.fluidcombat.helper.GuardStanceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
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
    private static boolean inGuardSetup = false;

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void tryBlockWithWeapon(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (inGuardSetup) return;
        if (!GuardStanceHelper.canUseGuardStance(player) || GuardStanceHelper.isGuarding(player)) return;

        inGuardSetup = true;
        try {
            GuardStanceHelper.startGuarding(player);

            MultiPlayerGameMode gm = Minecraft.getInstance().gameMode;
            gm.useItem(player, hand);       // server handshake
            player.startUsingItem(hand);    // client animation & use-timer
            cir.setReturnValue(InteractionResultHolder.consume(player.getItemInHand(hand)));
        } finally {
            inGuardSetup = false;
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void onGetUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        Player player = Minecraft.getInstance().player;

        // Quick null & context sanity check
        if (player != null && player.getMainHandItem() == stack) {
            if (GuardStanceHelper.canUseGuardStance(player)) {
                cir.setReturnValue(UseAnim.BLOCK);
            }
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void onGetUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem() == stack && GuardStanceHelper.isGuarding(player))
        {
            // match shield’s duration so client HOLD doesn’t time out
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void onReleaseUseItem(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (timeLeft < 72000) {
            GuardStanceHelper.stopGuarding(player);
        }
    }
}