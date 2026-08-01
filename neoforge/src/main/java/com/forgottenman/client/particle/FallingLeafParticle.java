package com.forgottenman.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A shed leaf */
public class FallingLeafParticle extends TextureSheetParticle {
    private final float swayPhase;
    private float spin;

    protected FallingLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.lifetime = 240 + this.random.nextInt(120);
        this.gravity = 0.02F;
        this.friction = 0.98F;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        float size = 0.12F + this.random.nextFloat() * 0.06F;
        this.quadSize = size;
        this.setSize(size, size);
        this.swayPhase = this.random.nextFloat() * (float) (Math.PI * 2.0);
        this.spin = (float) Math.toRadians(this.random.nextBoolean() ? -2.5 : 2.5);
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0);
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float t = this.age * 0.07F + this.swayPhase;
        this.xd += Math.cos(t) * 0.0012;
        this.zd += Math.sin(t * 0.83F) * 0.0012;
        this.spin += (float) Math.toRadians(this.random.nextBoolean() ? -0.2 : 0.2);
        this.oRoll = this.roll;
        this.roll += this.spin / 10.0F;
        if (this.onGround) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new FallingLeafParticle(level, x, y, z, this.sprites);
        }
    }
}
