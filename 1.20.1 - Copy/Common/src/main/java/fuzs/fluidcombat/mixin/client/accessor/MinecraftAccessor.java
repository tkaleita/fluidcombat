package fuzs.fluidcombat.mixin.client.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Invoker("continueAttack")
    void fluidcombat$continueAttack(boolean attacking);
    @Invoker("startAttack")
    boolean fluidcombat$startAttack();
}
