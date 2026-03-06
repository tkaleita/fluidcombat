package focuss.fluidcombat.mixin.client.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Invoker("ensureHasSentCarriedItem")
    void fluidcombat$callEnsureHasSentCarriedItem();

    @Invoker("startPrediction")
    void fluidcombat$callStartPrediction(ClientLevel clientLevel, PredictiveAction predictiveAction);
}