package fuzs.fluidcombat.forge;

import fuzs.fluidcombat.FluidCombat;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.forge.fml.common.Mod;

@Mod(FluidCombat.MOD_ID)
public class FluidCombatForge {

    public FluidCombatForge() {
        ModConstructor.construct(FluidCombat.MOD_ID, FluidCombat::new);
    }
}
