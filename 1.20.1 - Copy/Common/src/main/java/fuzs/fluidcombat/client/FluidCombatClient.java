package fuzs.fluidcombat.client;

import fuzs.fluidcombat.client.handler.AttributesTooltipHandler;
import fuzs.fluidcombat.client.handler.ShieldIndicatorHandler;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.event.v1.*;

public class FluidCombatClient implements ClientModConstructor {

    @Override
    public void onConstructMod() {
        registerHandlers();
    }

    private static void registerHandlers() {
        RenderGuiElementEvents.before(fuzs.puzzleslib.api.client.event.v1.RenderGuiElementEvents.CROSSHAIR).register(ShieldIndicatorHandler::onBeforeRenderGuiElement);
        RenderGuiElementEvents.after(fuzs.puzzleslib.api.client.event.v1.RenderGuiElementEvents.CROSSHAIR).register((minecraft, guiGraphics, tickDelta, screenWidth, screenHeight) -> ShieldIndicatorHandler.onAfterRenderGuiElement(RenderGuiElementEvents.CROSSHAIR, minecraft, guiGraphics, tickDelta, screenWidth, screenHeight));
        RenderGuiElementEvents.before(RenderGuiElementEvents.HOTBAR).register(ShieldIndicatorHandler::onBeforeRenderGuiElement);
        ItemTooltipCallback.EVENT.register(AttributesTooltipHandler::onItemTooltip);
    }
}