package fuzs.fluidcombat.mixin.client.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Invoker("ensureHasSentCarriedItem")
    void combatnouveau$callEnsureHasSentCarriedItem();
}
