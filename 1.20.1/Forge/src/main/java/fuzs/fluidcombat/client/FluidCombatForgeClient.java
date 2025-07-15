package fuzs.fluidcombat.client;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.helper.SweepAttackHelper;
import fuzs.fluidcombat.particles.CustomSweepParticleProvider;
import fuzs.fluidcombat.particles.CustomSweepReverseParticleProvider;
import fuzs.fluidcombat.particles.ModParticlesForge;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FluidCombat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FluidCombatForgeClient {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticlesForge.CUSTOM_SWEEP.get(), CustomSweepParticleProvider::new);
        event.registerSpriteSet(ModParticlesForge.CUSTOM_SWEEP_REVERSE.get(), CustomSweepReverseParticleProvider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ClientModConstructor.construct(FluidCombat.MOD_ID, FluidCombatClient::new);

        event.enqueueWork(() -> {
            SweepAttackHelper.initParticles(
                ModParticlesForge.CUSTOM_SWEEP.get(),
                ModParticlesForge.CUSTOM_SWEEP_REVERSE.get()
                );
            }
        );

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent tick) -> {
            if (tick.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (SweepAttackHelper.sweepParticle != null) {
                SweepAttackHelper.updateSweepAttackParticle(
                    SweepAttackHelper.sweepParticle,
                    mc.player,
                    mc.level
                );
            }
            if (SweepAttackHelper.secondarySweepParticle != null) {
                SweepAttackHelper.updateSweepAttackParticle(
                    SweepAttackHelper.secondarySweepParticle,
                    mc.player,
                    mc.level
                );
            }
        });
    }
}