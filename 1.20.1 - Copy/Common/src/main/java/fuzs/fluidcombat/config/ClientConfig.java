package fuzs.fluidcombat.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;
import fuzs.puzzleslib.api.config.v3.serialization.ConfigDataSet;
import net.minecraft.world.item.Item;

public class ClientConfig implements ConfigCore {
    @Config(description = "Render some default attributes such as armor protection with green text instead of blue one, just like with tools and weapons.")
    public boolean specialBaseAttributeModifiers = true;
    @Config(description = "Show a shield indicator similar to the attack indicator when actively blocking.")
    public boolean shieldIndicator = true;
    @Config(description = "Show particles on attack that show range and sweep radius.")
    public boolean showSweepTubeParticles = true;

    public ConfigDataSet<Item> hiddenOffhandItems;
}
