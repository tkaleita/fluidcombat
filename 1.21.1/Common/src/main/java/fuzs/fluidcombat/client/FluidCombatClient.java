package fuzs.fluidcombat.client;

import fuzs.fluidcombat.client.handler.AutoAttackHandler;
import fuzs.fluidcombat.client.handler.RenderOffhandItemHandler;
import fuzs.fluidcombat.client.handler.ShieldIndicatorHandler;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.event.v1.ClientTickEvents;
import fuzs.puzzleslib.api.client.event.v1.entity.player.InteractionInputEvents;
import fuzs.puzzleslib.api.client.event.v1.gui.RenderGuiLayerEvents;
import fuzs.puzzleslib.api.client.event.v1.renderer.RenderHandEvents;

public class FluidCombatClient implements ClientModConstructor {

    @Override
    public void onConstructMod() {
        registerEventHandlers();
    }

    private static void registerEventHandlers() {
        RenderGuiLayerEvents.before(RenderGuiLayerEvents.CROSSHAIR).register(
                ShieldIndicatorHandler::onBeforeRenderGuiLayer);
        RenderGuiLayerEvents.after(RenderGuiLayerEvents.CROSSHAIR).register(
                ShieldIndicatorHandler.onAfterRenderGuiLayer(RenderGuiLayerEvents.CROSSHAIR));
        RenderGuiLayerEvents.before(RenderGuiLayerEvents.HOTBAR).register(
                ShieldIndicatorHandler::onBeforeRenderGuiLayer);
        RenderGuiLayerEvents.after(RenderGuiLayerEvents.HOTBAR).register(
                ShieldIndicatorHandler.onAfterRenderGuiLayer(RenderGuiLayerEvents.HOTBAR));
        InteractionInputEvents.ATTACK.register(AutoAttackHandler::onAttackInteraction);
        ClientTickEvents.START.register(AutoAttackHandler::onStartTick);
        RenderHandEvents.OFF_HAND.register(RenderOffhandItemHandler::onRenderOffHand);
    }
}
