package fuzs.fluidcombat.helper;

import fuzs.fluidcombat.core.CommonAbstractions;
import fuzs.fluidcombat.particles.CustomSweepParticle;
import fuzs.fluidcombat.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SweepAttackHelper {

    public static Particle sweepParticle;
    public static float forwardOffset = 1.2f;
    public static float downwardOffset = 0.55f;
    public static float sidewaysOffset = 0.25f;

    public static void initiateSweepAttack(Player player) {
        if (!canSweepAttack(player)) return;
        float attackDamage = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        if (attackDamage > 0.0F) {
            double reach = player.getAttributeValue(CommonAbstractions.INSTANCE.getAttackRangeAttribute());

            var list = getEntitiesInSweepCone(player, reach);
            sweepAttack(player, list, attackDamage, null);
        }
        // also resets attack ticker
        player.swing(InteractionHand.MAIN_HAND);
    }
    
    private static void sweepAttack(Player player, List<LivingEntity> list, float attackDamage, @Nullable Entity target) {
        float sweepingAttackDamage = 1.0F + (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage;
        System.out.println(list);

        for (LivingEntity livingEntity : list) {
            DamageSource damageSource = player.damageSources().playerAttack(player);//player.damageSources().playerAttack(player);
            float enchantedDamage = player.getEnchantedDamage(livingEntity, sweepingAttackDamage, damageSource);
            var deltaMovement = livingEntity.getDeltaMovement(); // get current velocity to reset after hurt
            livingEntity.hurt(damageSource, enchantedDamage);
            livingEntity.setDeltaMovement(deltaMovement); // reset/cancel vanilla knockback (triggered by .hurt())
            livingEntity.knockback(0.1f, // apply our own knockback!!
                Mth.sin((float)(player.getYRot() * ((float) Math.PI / 180.0))),
                -Mth.cos((float)(player.getYRot() * ((float) Math.PI / 180.0))));
            if (player.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, livingEntity, damageSource);
            }
            // play hit sound
            player.level().playSound(
                null, // null = audible for all nearby players
                livingEntity.getX(),
                livingEntity.getY(),
                livingEntity.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK,
                SoundSource.PLAYERS,
                1.0F,  // volume
                0.9F + player.level().random.nextFloat() * 0.2F // slightly varied pitch
            );
        }
    }

    public static void spawnSweepAttackEffects(Player player, Level level) {
        Minecraft mc = Minecraft.getInstance();
        ParticleEngine pe = mc.particleEngine;

        // reverse or normal?
        var chosenSweep = Math.random() > 0.5f ? ModParticles.CUSTOM_SWEEP : ModParticles.CUSTOM_SWEEP_REVERSE;

        // actually spawn sweep attack particle
        var sweepParticlePositon = SweepAttackHelper.getSweepAttackPosition(player, forwardOffset, downwardOffset, sidewaysOffset);
        sweepParticle = pe.createParticle(chosenSweep,
            sweepParticlePositon.x(),
            sweepParticlePositon.y(),
            sweepParticlePositon.z(),
            0.0, 0.0, 0.0);

        // if weapon has fire aspect, make it ORANGE
        ItemStack stack = player.getMainHandItem();
        Registry<Enchantment> enchantmentRegistry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fireAspect = enchantmentRegistry.getHolderOrThrow(Enchantments.FIRE_ASPECT);
        int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(fireAspect, stack);
        if (fireAspectLevel > 0) {
            sweepParticle.setColor(1.0f, 0.4f, 0.1f); // fiery orange
        }

        // if crit is possible, turn particle red
        if (!player.onGround() && player.getDeltaMovement().y < 0.0) 
            sweepParticle.setColor(0.6f, 0.1f, 0.1f);

        // set angle of particle
        if (sweepParticle instanceof CustomSweepParticle) {
            ((CustomSweepParticle)sweepParticle).setAngle(0);
        }

        // play sweep attack sound
        float pitch = 0.75F + RandomSource.create().nextFloat() * 0.2F; // Random pitch between 0.65 and 0.85
        level.playSound(
            player,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS,
            0.50F, // volume
            pitch
        );
    }

    public static List<LivingEntity> getEntitiesInSweepCone(Player player, double reach) {
        double minSweep = 0.0;
        double maxSweep = 2.5;
        double normalizedReach = Mth.clamp((reach - 1.0) / (7.5 - 1.0), 0.0, 1.0);
        double radius = Mth.lerp(1.0 - normalizedReach, minSweep, maxSweep);
        System.out.println("reach:" + reach + "radius:" + radius);

        drawSweepTube(player, reach, radius);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();
        Level level = player.level();

        // Rough bounding box for nearby entity pre-filtering
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(reach*2)).inflate(radius*2);

        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox, e ->
            e != player && e.isPickable() && !e.isSpectator() && !player.isAlliedTo(e)
        );

        potentialTargets = potentialTargets.stream()
            .filter(entity -> {
                Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0); // center of body
                Vec3 toEntity = entityPos.subtract(eyePos);

                double alongRay = toEntity.dot(lookVec); // 1d projection of distance between us and the enemy
                if (alongRay < 0 || alongRay > reach) return false;

                double distanceFromPlayerSq = entityPos.distanceToSqr(eyePos); // no inflation of attack range at the edge of the "tube"
                if (distanceFromPlayerSq > reach * reach) return false;

                HitResult obstruction = player.level().clip(new ClipContext( // can we see the enemy?
                    eyePos,
                    entityPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
                ));
                if (obstruction.getType() == HitResult.Type.BLOCK) return false; 

                Vec3 closestPoint = eyePos.add(lookVec.scale(alongRay));
                double distanceSq = entityPos.distanceToSqr(closestPoint);

                double entityInflatedRadius = radius + (entity.getBbWidth() * 0.5); // to account for wide enemies, give some leeway
                return distanceSq <= entityInflatedRadius * entityInflatedRadius;
            })
            .toList();
            potentialTargets = new ArrayList<>(potentialTargets);

            // limit entities hit
            int baseLimit = 3;
            Registry<Enchantment> enchantmentRegistry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> sweepingEdge = enchantmentRegistry.getHolderOrThrow(Enchantments.SWEEPING_EDGE);
            int sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, player.getMainHandItem());
            int maxTargets = baseLimit + sweepingLevel;

            // Sort by distance
            potentialTargets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

            // Trim to maxTargets
            if (potentialTargets.size() > maxTargets) {
                potentialTargets = potentialTargets.subList(0, maxTargets);
            }
            return potentialTargets;
    }

    public static void drawSweepTube(Player player, double reach, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        int steps = 20;
        double stepSize = reach / steps;

        var particle = new DustParticleOptions(
            new Vector3f(0.75F, 0.75F, 0.75F), // RGB (white in this case)
            0.5F // size (default is 1.0F)
        );

        for (int i = 0; i <= steps; i++) {
            Vec3 point = eye.add(look.scale(i * stepSize));

            // Particle cloud at each step
            drawCircleParticles(serverLevel, particle, point, look, radius, 8);
        }
    }

    private static void drawCircleParticles(ServerLevel level, DustParticleOptions particle, Vec3 center, Vec3 forward, double radius, int segments) {
        // Local coordinate frame for rotating around forward
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(forward.dot(up)) > 0.99) up = new Vec3(1, 0, 0); // Avoid near-parallel
        Vec3 right = forward.cross(up).normalize();
        Vec3 perpUp = right.cross(forward).normalize();

        for (int i = 0; i < segments; i++) {
            double angle = (2 * Math.PI / segments) * i;
            Vec3 offset = right.scale(Math.cos(angle)).add(perpUp.scale(Math.sin(angle))).scale(radius);
            Vec3 particlePos = center.add(offset);

            level.sendParticles(particle, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
        }
    }

    public static boolean updateSweepAttackParticle(Player player, Level level) {
        if (sweepParticle == null) return false;

        var newPos = getSweepAttackPosition(player, forwardOffset, downwardOffset, sidewaysOffset);
        sweepParticle.setPos(newPos.x(), newPos.y(), newPos.z());
        return true;
    }

    public static Vec3 getSweepAttackPosition(Player player, double forwardOffset, double downwardOffset, double sidewaysOffset) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle().normalize();

        Vec3 upVector = new Vec3(0.0, 1.0, 0.0);
        Vec3 rightVector = lookVector.cross(upVector).normalize();
        Vec3 downScreenVector = rightVector.cross(lookVector).normalize();

        Vec3 forwardPosition = eyePosition.add(lookVector.scale(forwardOffset));
        Vec3 downwardAdjusted = forwardPosition.add(downScreenVector.scale(-downwardOffset));
        Vec3 sidewaysAdjusted = downwardAdjusted.add(rightVector.scale(sidewaysOffset));

        return sidewaysAdjusted;
    }

     private static boolean canSweepAttack(Player player) {
        ItemStack stack = player.getMainHandItem();
        var components = stack.getComponents();

        if (!components.has(DataComponents.ATTRIBUTE_MODIFIERS)) return false;

        var modifiers = components.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;

        return modifiers.modifiers().stream().anyMatch(x -> x.attribute() == Attributes.ATTACK_DAMAGE);
    }
}