package fuzs.combatnouveau.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class CustomSweepParticle extends TextureSheetParticle {

    public final SpriteSet sprites;
    public int fixedBrightness;

    public CustomSweepParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        sprites = spriteSet;
        
        // initial setup
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.quadSize = 1.0F; // Try values between 0.5F and 2.0F
        this.lifetime = 3;
        this.hasPhysics = false;

        // fixed brightness
        this.fixedBrightness = super.getLightColor(0.0f);

        // done
        this.setSprite(spriteSet.get(0, this.lifetime));
    }

    @Override
    public int getLightColor(float partialTick) {
        return this.fixedBrightness;
    }

    public void setAngle(float angle) {
        float angleVariation = 10;
        angle += Math.round((Math.random()*angleVariation)-(angleVariation/2)); // angle +-x degrees
        float fixedAngle = (float) Math.toRadians(angle);
        this.roll = fixedAngle;
        this.oRoll = fixedAngle;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}