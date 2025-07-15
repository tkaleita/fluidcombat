package fuzs.fluidcombat.particles;

import fuzs.fluidcombat.FluidCombat;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModParticlesForge {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, FluidCombat.MOD_ID);

    public static final RegistryObject<SimpleParticleType> CUSTOM_SWEEP =
        PARTICLE_TYPES.register("custom_sweep", () -> new SimpleParticleType(false));
        
    public static final RegistryObject<SimpleParticleType> CUSTOM_SWEEP_REVERSE =
        PARTICLE_TYPES.register("custom_sweep_reverse", () -> new SimpleParticleType(false));
}
