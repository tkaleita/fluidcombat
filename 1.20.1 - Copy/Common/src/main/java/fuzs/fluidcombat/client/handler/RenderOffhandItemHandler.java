package fuzs.fluidcombat.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.config.ClientConfig;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public class RenderOffhandItemHandler {

    public static EventResult onRenderOffHand(ItemInHandRenderer itemInHandRenderer, AbstractClientPlayer player, HumanoidArm humanoidArm, ItemStack itemStack, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLight, float partialTick, float interpolatedPitch, float swingProgress, float equipProgress) {
        if (!itemStack.isEmpty() && FluidCombat.CONFIG.get(ClientConfig.class).hiddenOffhandItems.contains(itemStack.getItem())) {
            if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.OFF_HAND ||
                    itemStack.getItem() instanceof ShieldItem && FluidCombat.CONFIG.get(ClientConfig.class).shieldIndicator) {
                return EventResult.INTERRUPT;
            }
        }
        return EventResult.PASS;
    }
}
