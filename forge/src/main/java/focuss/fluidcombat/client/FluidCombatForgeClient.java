package focuss.fluidcombat.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.helper.FluidCombatHelper;
import focuss.fluidcombat.particles.CustomSweepParticleProvider;
import focuss.fluidcombat.particles.CustomSweepReverseParticleProvider;
import focuss.fluidcombat.particles.ModParticlesForge;
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
            FluidCombatHelper.initParticles(
                ModParticlesForge.CUSTOM_SWEEP.get(),
                ModParticlesForge.CUSTOM_SWEEP_REVERSE.get()
                );
            }
        );

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent tick) -> {
            if (tick.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (FluidCombatHelper.sweepParticle != null) {
                FluidCombatHelper.updateSweepAttackParticle(
                    FluidCombatHelper.sweepParticle,
                    mc.player
                );
            }
            if (FluidCombatHelper.secondarySweepParticle != null) {
                FluidCombatHelper.updateSweepAttackParticle(
                    FluidCombatHelper.secondarySweepParticle,
                    mc.player
                );
            }

            // preview sweep targets every X ticks!
            /*if (mc.player.tickCount % 3 == 0) {
                SweepAttackHelper.previewSweepTargets(mc.player);
            }*/
        });
    }
}