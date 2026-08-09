package com.forgottenman.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import com.forgottenman.client.render.LateLeafParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (!LateLeafParticles.active()) {
            super.render(buffer, camera, partialTicks);
            return;
        }
        // Under a shaderpack, hand the quad to LateLeafParticles instead of the particle
        // buffer so it can be redrawn unlit after the pack composites. Same billboard maths
        // vanilla uses, so the leaves sit exactly where they otherwise would.
        Vec3 camPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        if (this.roll != 0.0F) {
            rotation.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }
        float size = getQuadSize(partialTicks);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        float[] corners = {-1.0F, -1.0F, u1, v1, -1.0F, 1.0F, u1, v0,
                1.0F, 1.0F, u0, v0, 1.0F, -1.0F, u0, v1};
        float[] quad = new float[20];
        for (int i = 0; i < 4; i++) {
            Vector3f corner = new Vector3f(corners[i * 4], corners[i * 4 + 1], 0.0F);
            corner.rotate(rotation).mul(size).add(x, y, z);
            quad[i * 5] = corner.x();
            quad[i * 5 + 1] = corner.y();
            quad[i * 5 + 2] = corner.z();
            quad[i * 5 + 3] = corners[i * 4 + 2];
            quad[i * 5 + 4] = corners[i * 4 + 3];
        }
        LateLeafParticles.submit(quad, new float[]{this.rCol, this.gCol, this.bCol, this.alpha});
    }
}
