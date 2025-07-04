package fuzs.fluidcombat.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class CustomSweepReverseParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet spriteSet;

    public CustomSweepReverseParticleProvider(SpriteSet spriteSet) {
        this.spriteSet = spriteSet;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                   double xd, double yd, double zd) {
        xd = 0.0;
        yd = 0.0;
        zd = 0.0;
        return new CustomSweepParticle(level, x, y, z, xd, yd, zd, spriteSet);
    }
}