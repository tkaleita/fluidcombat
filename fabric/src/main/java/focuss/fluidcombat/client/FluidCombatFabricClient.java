package focuss.fluidcombat.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.helper.FluidCombatHelper;
import focuss.fluidcombat.particles.CustomSweepParticleProvider;
import focuss.fluidcombat.particles.CustomSweepReverseParticleProvider;
import focuss.fluidcombat.particles.ModParticles;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.event.v1.ClientTickEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class FluidCombatFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientModConstructor.construct(FluidCombat.MOD_ID, FluidCombatClient::new);
        
        ParticleFactoryRegistry.getInstance().register(
            ModParticles.CUSTOM_SWEEP,
            CustomSweepParticleProvider::new
        );

        ParticleFactoryRegistry.getInstance().register(
            ModParticles.CUSTOM_SWEEP_REVERSE,
            CustomSweepReverseParticleProvider::new
        );

        // update sweep particle on every tick if it exists
        ClientTickEvents.END.register(client -> {
            if (FluidCombatHelper.sweepParticle != null && client.player != null) {
                FluidCombatHelper.updateSweepAttackParticle(FluidCombatHelper.sweepParticle, client.player);
            }
            if (FluidCombatHelper.secondarySweepParticle != null && client.player != null) {
                FluidCombatHelper.updateSweepAttackParticle(FluidCombatHelper.secondarySweepParticle, client.player);
            }
        });
    }
}
