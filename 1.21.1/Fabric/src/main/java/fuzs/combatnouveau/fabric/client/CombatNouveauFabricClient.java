package fuzs.combatnouveau.fabric.client;

import fuzs.combatnouveau.CombatNouveau;
import fuzs.combatnouveau.client.CombatNouveauClient;
import fuzs.combatnouveau.helper.SweepAttackHelper;
import fuzs.combatnouveau.particles.CustomSweepParticleProvider;
import fuzs.combatnouveau.particles.ModParticles;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.event.v1.ClientTickEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class CombatNouveauFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientModConstructor.construct(CombatNouveau.MOD_ID, CombatNouveauClient::new);
        
        ParticleFactoryRegistry.getInstance().register(
            ModParticles.CUSTOM_SWEEP,
            CustomSweepParticleProvider::new
        );

        ParticleFactoryRegistry.getInstance().register(
            ModParticles.CUSTOM_SWEEP_REVERSE,
            CustomSweepParticleProvider::new
        );


        // update sweep particle on every tick if it exists
        ClientTickEvents.END.register(client -> {
            if (SweepAttackHelper.sweepParticle != null && client.player != null) {
                SweepAttackHelper.updateSweepAttackParticle(client.player, client.level);
            }
        });
    }
}
