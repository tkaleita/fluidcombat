package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import focuss.fluidcombat.helper.GuardStanceHelper;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.Cancellable;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void injectCustomGuardPose(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                       float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (GuardStanceHelper.isGuarding(player) &&
                hand == InteractionHand.MAIN_HAND) {//player.getUsedItemHand()) {

            //float sign = hand == InteractionHand.MAIN_HAND ? 1.0F : -1.0F;

            poseStack.translate(-0.17, 0.53, -0.36);
            poseStack.mulPose(Axis.XP.rotationDegrees(88));
            poseStack.mulPose(Axis.YP.rotationDegrees(154));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-98));

        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float removeEquipDip(LocalPlayer instance, float v) {
        return 1.0F;
    }

}