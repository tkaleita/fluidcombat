package focuss.fluidcombat.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;
import fuzs.puzzleslib.api.config.v3.serialization.ConfigDataSet;
import net.minecraft.world.item.Item;

public class ClientConfig implements ConfigCore {
    @Config(description = "Show a shield indicator similar to the attack indicator when actively blocking.")
    public boolean shieldIndicator = true;
    @Config(description = "Show particles on attack that show range and sweep radius.")
    public boolean showSweepTubeParticles = true;
    @Config(description = "Show sweep attack particle on each attack.")
    public boolean showSweepAttackParticles = true;
}
