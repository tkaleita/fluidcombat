package fuzs.fluidcombat.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;
import fuzs.puzzleslib.api.config.v3.serialization.ConfigDataSet;
import fuzs.puzzleslib.api.config.v3.serialization.KeyedValueProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public class ClientConfig implements ConfigCore {
    @Config(description = "Render some default attributes such as armor protection with green text instead of blue one, just like with tools and weapons.")
    public boolean specialBaseAttributeModifiers = true;
    @Config(description = "Show a shield indicator similar to the attack indicator when actively blocking.")
    public boolean shieldIndicator = true;
    @Config(name = "hidden_offhand_items", description = "Items that will not be rendered as being held in first person when placed in the offhand.")
    List<String> hiddenOffhandItemsRaw = KeyedValueProvider.toString(Registries.ITEM, Items.TOTEM_OF_UNDYING);
    @Config(description = "Show particles on attack that show range and sweep radius.")
    public boolean showSweepTubeParticles = true;

    public ConfigDataSet<Item> hiddenOffhandItems;

    @Override
    public void afterConfigReload() {
        this.hiddenOffhandItems = ConfigDataSet.from(Registries.ITEM, this.hiddenOffhandItemsRaw);
    }
}
