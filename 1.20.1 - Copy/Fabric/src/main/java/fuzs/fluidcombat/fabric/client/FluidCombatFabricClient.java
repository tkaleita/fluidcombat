package fuzs.fluidcombat.fabric.client;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.client.FluidCombatClient;
import fuzs.fluidcombat.helper.SweepAttackHelper;
import fuzs.fluidcombat.particles.CustomSweepParticleProvider;
import fuzs.fluidcombat.particles.CustomSweepReverseParticleProvider;
import fuzs.fluidcombat.particles.ModParticles;
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
            if (SweepAttackHelper.sweepParticle != null && client.player != null) {
                SweepAttackHelper.updateSweepAttackParticle(SweepAttackHelper.sweepParticle, client.player, client.level);
            }
            if (SweepAttackHelper.secondarySweepParticle != null && client.player != null) {
                SweepAttackHelper.updateSweepAttackParticle(SweepAttackHelper.secondarySweepParticle, client.player, client.level);
            }
        });
    }
}
