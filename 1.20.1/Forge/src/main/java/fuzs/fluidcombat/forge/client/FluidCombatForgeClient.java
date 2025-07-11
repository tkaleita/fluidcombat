package fuzs.fluidcombat.forge.client;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.client.FluidCombat;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import net.forge.api.distmarker.Dist;
import net.forge.fml.common.Mod;

@Mod(value = FluidCombat.MOD_ID, dist = Dist.CLIENT)
public class FluidCombatForgeClient {


    public FluidCombatForgeClient() {
        ClientModConstructor.construct(FluidCombat.MOD_ID, FluidCombatClient::new);
        MinecraftForge.EVENT_BUS.register(this); // Needed for @SubscribeEvent to work!
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        Minecraft mc = Minecraft.getInstance();

        mc.particleEngine.register(ModParticles.CUSTOM_SWEEP.get(), CustomSweepParticleProvider::new);
        mc.particleEngine.register(ModParticles.CUSTOM_SWEEP_REVERSE.get(), CustomSweepReverseParticleProvider::new);
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