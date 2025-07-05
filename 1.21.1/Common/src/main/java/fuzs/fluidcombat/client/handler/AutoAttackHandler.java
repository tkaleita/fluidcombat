package fuzs.fluidcombat.client.handler;

import fuzs.fluidcombat.config.ServerConfig;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

public class AutoAttackHandler {
    public static boolean readyForAutoAttack(LocalPlayer player) {
        return player.getAttackStrengthScale(0.5F) >= 1.0F;
    }

    public static EventResult onAttackInteraction(Minecraft minecraft, LocalPlayer player, HitResult hitResult) {
        if (minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            if (player.getAttackStrengthScale(0.5F) < fuzs.fluidcombat.FluidCombat.CONFIG.get(ServerConfig.class).minAttackStrength) {
                return EventResult.INTERRUPT;
            }
        }
        return EventResult.PASS;
    }
}