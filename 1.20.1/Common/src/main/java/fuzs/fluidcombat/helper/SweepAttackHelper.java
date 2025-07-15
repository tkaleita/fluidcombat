package fuzs.fluidcombat.helper;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.config.ClientConfig;
import fuzs.fluidcombat.core.CommonAbstractions;
import fuzs.fluidcombat.particles.CustomSweepParticle;
import fuzs.fluidcombat.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SweepAttackHelper {

    public static Particle sweepParticle;
    public static Particle secondarySweepParticle;

    public static float forwardOffset = 1.2f;
    public static float downwardOffset = 0.55f;
    public static float sidewaysOffset = 0.25f;

    public static double minSweep = 0.25;
    public static double maxSweep = 2;

    public static void initiateSweepAttack(Player player) {
        if (!canSweepAttack(player)) return;

        var target = Minecraft.getInstance().crosshairPickEntity;
        float attackDamage = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        if (attackDamage > 0.0F) {
            double reach = player.getAttributeValue(CommonAbstractions.INSTANCE.getAttackRangeAttribute());
            player.displayClientMessage(
                Component.literal("[SweepDebug] Reach: " + reach), 
                false  // false = chat, true = action bar
            );
            var list = getEntitiesInSweepCone(player, reach);
            sweepAttack(player, list, attackDamage, target);
        }
        // also resets attack ticker
        player.swing(InteractionHand.MAIN_HAND);
    }
    
    private static void sweepAttack(Player player, List<Entity> list, float attackDamage, @Nullable Entity target) {
        float sweepingDamageRatio = 0.5f; // older Minecraft default
        //float sweepingAttackDamage = 1.0F + (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage;
        float sweepingAttackDamage = 1.0F;
        DamageSource damageSource = player.damageSources().playerAttack(player);

        for (Entity entity : list) {
            var isCrit = !player.onGround() && player.getDeltaMovement().y < -0.15; // its a crit if player is falling... downwards

            if (target != null && entity.getUUID().equals(target.getUUID())) {
                continue;
            };
            
            //float enchantedSweepDamage = player.getEnchantedDamage(entity, sweepingAttackDamage, damageSource);
            float enchantedSweepDamage = sweepingAttackDamage;
            if (entity instanceof LivingEntity livingEntity) {
                float bonus = EnchantmentHelper.getDamageBonus(player.getMainHandItem(), livingEntity.getMobType());
                enchantedSweepDamage += bonus;
            }

            var sound = SoundEvents.PLAYER_ATTACK_WEAK;
            if (isCrit) {
                sound = SoundEvents.PLAYER_ATTACK_STRONG;
                enchantedSweepDamage *= 1.5;
                if (player.level() instanceof ServerLevel serverLevel)
                    serverLevel.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 5, 0, 0, 0, 1);
            } 
            
            var deltaMovement = entity.getDeltaMovement(); // get current velocity to reset after hurt
            entity.hurt(damageSource, enchantedSweepDamage);
            entity.setDeltaMovement(deltaMovement); // reset/cancel vanilla knockback (triggered by .hurt())

            if (entity instanceof LivingEntity living) {
                living.knockback(0.1f, // apply our own knockback!!
                    Mth.sin((float)(player.getYRot() * ((float) Math.PI / 180.0))),
                    -Mth.cos((float)(player.getYRot() * ((float) Math.PI / 180.0))));
            } else if (entity instanceof EnderDragonPart part) {
                EnderDragon parent = part.parentMob;
                parent.hurt(part, damageSource, enchantedSweepDamage);
            }

            if (player.level() instanceof ServerLevel serverLevel && entity instanceof LivingEntity livingEntity) {
                EnchantmentHelper.doPostHurtEffects(livingEntity, player);
                EnchantmentHelper.doPostDamageEffects(player, livingEntity);
            }

            // play hit sound
            player.level().playSound(
                null, // null = audible for all nearby players
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0F,  // volume
                0.9F + player.level().random.nextFloat() * 0.2F // slightly varied pitch
            );
        }
    }

    private static SimpleParticleType sweep, sweepReverse;
    public static void initParticles(SimpleParticleType normal, SimpleParticleType reverse) {
        sweep = normal;
        sweepReverse = reverse;
    }
    public static void spawnSweepAttackEffects(Player player, Level level) {
        Minecraft mc = Minecraft.getInstance();
        ParticleEngine pe = mc.particleEngine;

        // reverse or normal?
        var chosenSweep = Math.random() > 0.5f ? sweep : sweepReverse;

        // actually spawn sweep attack particle
        sweepParticle = pe.createParticle(chosenSweep,
            player.position().x(), // spawn it inside player first, so lighting matches up
            player.position().y(), // otherwise, particle can spawn inside blocks, making it look too dark
            player.position().z(),
            0.0, 0.0, 0.0);

        // if weapon has fire aspect, make it ORANGE
        ItemStack stack = player.getMainHandItem();
        int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        if (fireAspectLevel > 0) {
            // spawn a smaller secondary particle!!
            secondarySweepParticle = pe.createParticle(chosenSweep,
            player.position().x(),
            player.position().y(), 
            player.position().z(),
            0.0, 0.0, 0.0);
            secondarySweepParticle.scale(0.75f);
            secondarySweepParticle.setColor(1.0f, 0.4f, 0.1f); // fiery orange

        }

        // if crit is possible, turn particle red
        if (!player.onGround() && player.getDeltaMovement().y < -0.15) 
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

    public static List<Entity> getEntitiesInSweepCone(Player player, double reach) {
        double normalizedReach = Mth.clamp((reach - 1.0) / (6.0 - 1.0), 0.0, 1.0);
        double radius = Mth.lerp(1.0 - normalizedReach, minSweep, maxSweep);

        drawSweepTube(player, reach, radius);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();
        Level level = player.level();

        // Rough bounding box for nearby entity pre-filtering
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(reach*2)).inflate(radius*2);

        List<Entity> potentialTargets = level.getEntitiesOfClass(Entity.class, searchBox, e ->
            e != player && e.isPickable() && !e.isSpectator() && !player.isAlliedTo(e)
        );

        potentialTargets = potentialTargets.stream()
            .filter(entity -> {
                if ((!(entity instanceof LivingEntity || entity instanceof EnderDragonPart)) || entity == player || !entity.isPickable() || entity.isSpectator()) {
                    return false; // can we even attack this thing? if no, return false
                }
                // pet-check (doesnt fit in above filter)
                if (entity instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) return false;

                AABB bb = entity.getBoundingBox();
                Vec3 entityPos = new Vec3(
                    Mth.clamp(eyePos.x, bb.minX, bb.maxX),
                    Mth.clamp(eyePos.y, bb.minY, bb.maxY),
                    Mth.clamp(eyePos.z, bb.minZ, bb.maxZ)
                );

                Vec3 toEntity = entityPos.subtract(eyePos);

                // DEBUG
                /*if ((player.level() instanceof ServerLevel serverLevel))
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, entityPos.x, entityPos.y, entityPos.z, 1, 0, 0, 0, 0);*/

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
            int baseLimit = 3;;
            int sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, player.getMainHandItem());
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

    public static boolean updateSweepAttackParticle(Particle sweepParticle, Player player, Level level) {
        if (sweepParticle == null) return false;
        
        Vec3 newPos = null;
        if (sweepParticle == secondarySweepParticle) {
            newPos = getSweepAttackPosition(player, forwardOffset-0.05f, downwardOffset, sidewaysOffset);
        } else {
            newPos = getSweepAttackPosition(player, forwardOffset, downwardOffset, sidewaysOffset);
        }
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

    public static boolean canSweepAttack(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;

        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.isEmpty()) return false;

        return modifiers.containsKey(Attributes.ATTACK_DAMAGE);
    }

}