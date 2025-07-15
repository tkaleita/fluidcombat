package fuzs.fluidcombat.fabric;

import fuzs.fluidcombat.FluidCombat;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.fabricmc.api.ModInitializer;

public final class FluidCombatFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConstructor.construct(FluidCombat.MOD_ID, FluidCombat::new);
    }
}