package fuzs.combatnouveau.particles;

import fuzs.combatnouveau.CombatNouveau;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModParticles {
        public static final SimpleParticleType CUSTOM_SWEEP = new SimpleParticleType(true);
        public static final SimpleParticleType CUSTOM_SWEEP_REVERSE = new SimpleParticleType(true);

        public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
            CombatNouveau.id("custom_sweep"),
            CUSTOM_SWEEP);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
            CombatNouveau.id("custom_sweep_reverse"),
            CUSTOM_SWEEP_REVERSE);
    }
}
