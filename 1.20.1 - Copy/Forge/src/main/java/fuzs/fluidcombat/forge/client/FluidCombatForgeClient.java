package fuzs.fluidcombat.forge.client;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.client.FluidCombatClient;
import fuzs.fluidcombat.helper.SweepAttackHelper;
import fuzs.fluidcombat.particles.CustomSweepParticleProvider;
import fuzs.fluidcombat.particles.CustomSweepReverseParticleProvider;
import fuzs.fluidcombat.particles.ModParticles;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = FluidCombat.MOD_ID)
public class FluidCombatForgeClient {


    public FluidCombatForgeClient() {
        ClientModConstructor.construct(FluidCombat.MOD_ID, FluidCombatClient::new);
        MinecraftForge.EVENT_BUS.register(this); // Needed for @SubscribeEvent to work!
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        Minecraft mc = Minecraft.getInstance();

        mc.particleEngine.register(ModParticles.CUSTOM_SWEEP, CustomSweepParticleProvider::new);
        mc.particleEngine.register(ModParticles.CUSTOM_SWEEP_REVERSE, CustomSweepReverseParticleProvider::new);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (SweepAttackHelper.sweepParticle != null && mc.player != null) {
            SweepAttackHelper.updateSweepAttackParticle(SweepAttackHelper.sweepParticle, mc.player, mc.level);
        }
        if (SweepAttackHelper.secondarySweepParticle != null && mc.player != null) {
            SweepAttackHelper.updateSweepAttackParticle(SweepAttackHelper.secondarySweepParticle, mc.player, mc.level);
        }
    }
}