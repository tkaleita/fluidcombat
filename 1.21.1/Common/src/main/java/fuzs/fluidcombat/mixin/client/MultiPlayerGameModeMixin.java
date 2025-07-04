package fuzs.fluidcombat.mixin.client;

import fuzs.fluidcombat.helper.SweepAttackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void onClientAttack(Player player, Entity target, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            SweepAttackHelper.spawnSweepAttackEffects(Minecraft.getInstance().player, Minecraft.getInstance().level);
        }
    }
}