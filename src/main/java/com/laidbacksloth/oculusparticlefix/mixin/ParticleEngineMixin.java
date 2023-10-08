package com.laidbacksloth.oculusparticlefix.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.coderbot.iris.fantastic.ParticleRenderingPhase;
import net.coderbot.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(value = ParticleEngine.class, priority = 9999)
public class ParticleEngineMixin implements PhasedParticleEngine {
    @Shadow @Final private static List<ParticleRenderType> RENDER_ORDER;

    @Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;

    private ParticleRenderingPhase phase = ParticleRenderingPhase.EVERYTHING;

    private static final List<ParticleRenderType> OPAQUE_PARTICLE_RENDER_TYPES;

    static {
        OPAQUE_PARTICLE_RENDER_TYPES = ImmutableList.of(
                ParticleRenderType.PARTICLE_SHEET_OPAQUE,
                ParticleRenderType.PARTICLE_SHEET_LIT,
                ParticleRenderType.CUSTOM,
                ParticleRenderType.NO_RENDER
        );
    }

    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;"), remap = false)
    private Set<ParticleRenderType> oculusfix$getParticlesToRenderKeySet(Map<ParticleRenderType, Queue<Particle>> instance) {
        return oculusfix$getParticlesToRender().keySet();
    }

    //@Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    //private Object oculusfix$getParticlesToRenderGet(Map<ParticleRenderType, Queue<Particle>> instance, Object o) {
    //    return oculusfix$getParticlesToRender().values();
    //}

    @Unique
    public Map<ParticleRenderType, Queue<Particle>> oculusfix$getParticlesToRender() {
        Map<ParticleRenderType, Queue<Particle>> toRender = Maps.newTreeMap(ForgeHooksClient.makeParticleRenderTypeComparator(RENDER_ORDER));
        for (Map.Entry<ParticleRenderType, Queue<Particle>> type : particles.entrySet()) {
            if (!(
                    (phase == ParticleRenderingPhase.TRANSLUCENT && OPAQUE_PARTICLE_RENDER_TYPES.contains(type.getKey()))
                            || (phase == ParticleRenderingPhase.OPAQUE && type.getKey() == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT))
            ) {
                toRender.put(type.getKey(), type.getValue());
            }
        }
        return toRender;
    }

    @Override
    public void setParticleRenderingPhase(ParticleRenderingPhase particleRenderingPhase) {
        phase = particleRenderingPhase;
    }
}
