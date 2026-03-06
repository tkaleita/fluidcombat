package focuss.fluidcombat.mixin.accessor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerAccessor {

    @Accessor("lastItemInMainHand")
    ItemStack fluidcombat$getLastItemInMainHand();

    @Accessor("lastItemInMainHand")
    void fluidcombat$setLastItemInMainHand(ItemStack lastItemInMainHand);
}
