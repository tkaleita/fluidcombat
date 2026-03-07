package focuss.fluidcombat.helper;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.config.ClientConfig;
import focuss.fluidcombat.config.ServerConfig;
import focuss.fluidcombat.core.CommonAbstractions;
import focuss.fluidcombat.mixin.client.accessor.LivingEntityAccessor;
import focuss.fluidcombat.particles.CustomSweepParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
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
import java.util.Vector;

public class SweepAttackHelper {

    public static Particle sweepParticle;
    public static Particle secondarySweepParticle;

    public static float forwardOffset = 1.2f;
    public static float downwardOffset = 0.55f;
    public static float sidewaysOffset = 0.25f;

    public static double minSweep = 0.25;
    public static double maxSweep = 2;

    public static EquipmentSlot lastUsedSlot = EquipmentSlot.MAINHAND;

    public static void initiateSweepAttack(Player player, EquipmentSlot slot) {
        if (!canSweepAttack(player, slot)) return;
        /*
        if (slot == EquipmentSlot.MAINHAND)
            FluidCombat.LOGGER.info("MAINHAND ATTACK WITH: {}", player.getItemBySlot(slot).getDisplayName());
        else if (slot == EquipmentSlot.OFFHAND)
            FluidCombat.LOGGER.info("OFFHAND ATTACK WITH: {}", player.getItemBySlot(slot).getDisplayName());
         */

        // client side reference
        var target = Minecraft.getInstance().crosshairPickEntity;

        // get damage
        ItemStack weapon = player.getItemBySlot(slot);
        double damage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        // apply weapon modifiers
        Multimap<Attribute, AttributeModifier> modifiers =
                weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier mod : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            switch (mod.getOperation()) {
                case ADDITION -> damage += mod.getAmount();
                case MULTIPLY_BASE -> damage += damage * mod.getAmount();
                case MULTIPLY_TOTAL -> damage *= (1 + mod.getAmount());
            }
        }
        float attackDamage = (float) damage;
        //FluidCombat.LOGGER.info("calculated damage: {}", attackDamage);

        // manually trigger attack if offhand
        if (slot == EquipmentSlot.OFFHAND && target instanceof LivingEntity) {
            performOffhandAttack(player, player.level().getEntity(target.getId()));
        }

        if (attackDamage > 1f) {
            double reach = player.getAttributeValue(CommonAbstractions.INSTANCE.getAttackRangeAttribute());
            var list = getEntitiesInSweepCone(player, reach);
            if (FluidCombat.CONFIG.get(ClientConfig.class).showSweepTubeParticles)
                drawSweepTube(player, reach);
            sweepAttack(player, list, attackDamage, slot, target);
        }
        // also resets attack ticker
        player.swing(slot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public static void performOffhandAttack(Player player, Entity target) {
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        var attributes = player.getAttributes();
        var mainMods = main.getAttributeModifiers(EquipmentSlot.MAINHAND);
        var offMods = off.getAttributeModifiers(EquipmentSlot.MAINHAND);
        int originalTicker = accessor.fluidcombat$getAttackStrengthTicker();

        try {
            // swap attribute modifiers
            attributes.removeAttributeModifiers(mainMods);
            attributes.addTransientAttributeModifiers(offMods);
            // swap visible weapon for enchantments / durability logic
            player.setItemSlot(EquipmentSlot.MAINHAND, off);
            // ensure full cooldown
            accessor.fluidcombat$setAttackStrengthTicker(100);
            // run vanilla attack
            player.attack(target);

        } finally {
            // restore attributes
            attributes.removeAttributeModifiers(offMods);
            attributes.addTransientAttributeModifiers(mainMods);
            // restore weapon
            player.setItemSlot(EquipmentSlot.MAINHAND, main);
            accessor.fluidcombat$setAttackStrengthTicker(originalTicker);
        }
    }

    public static boolean isPlayerCritting(Player player) {
        // TODO extra case for config "crits while sprinting
        return !player.onGround() && player.getDeltaMovement().y < -0.15 && !player.isInWater() && !player.isPassenger();
    }

    private static void sweepAttack(Player player, List<Entity> list, float baseDamage, EquipmentSlot slot, @Nullable Entity target) {
        //FluidCombat.LOGGER.info("sweepAttack with: {}", player.getItemBySlot(slot));
        float sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, player.getItemBySlot(slot));

        float sweepingAttackDamage = baseDamage * (0.1f+(sweepingLevel*0.1f));
        DamageSource damageSource = player.damageSources().playerAttack(player);

        for (Entity entity : list) {

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
            if (isPlayerCritting(player)) {
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
                0.9f + player.level().random.nextFloat() * 0.2F // slightly varied pitch
            );
        }
    }

    /// this is an estimation and copies vanilla minecraft logic. dont use for actual combat!
    public static float getItemAttackDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1f; // fist
        }
        double modifier = 0.0;
        var mods = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);
        for (var mod : mods) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                modifier += mod.getAmount();
            }
        }
        return (float)(1.0 + modifier);
    }

    private static SimpleParticleType sweep, sweepReverse;
    public static void initParticles(SimpleParticleType normal, SimpleParticleType reverse) {
        sweep = normal;
        sweepReverse = reverse;
    }
    public static void spawnSweepAttackEffects(Player player, Level level, EquipmentSlot slot, float damage, int lifetime) {
        // prepare for playing sweep sound
        float minDamage = 1.0f;
        float maxDamage = 10.0f;
        float normalizedDamage = Mth.clamp((damage - minDamage) / (maxDamage - minDamage), 0f, 1f);

        float maxPitch = 1f; // weak weapon
        float minPitch = 0.25f; // heavy weapon
        float weaponPitch = Mth.lerp(normalizedDamage, maxPitch, minPitch);

        lifetime /= 4; // cause otherwise. very slow

        // play sweep attack sound
        float pitch = weaponPitch + RandomSource.create().nextFloat() * 0.1F; // Random pitch between 0.65 and 0.85
        FluidCombat.LOGGER.info("damage: {} | pitch: {}", damage, pitch);
        level.playSound(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.35f, // volume
                pitch
        );

        if (!FluidCombat.CONFIG.get(ClientConfig.class).showSweepAttackParticles) return;

        Minecraft mc = Minecraft.getInstance();
        ParticleEngine pe = mc.particleEngine;

        // reverse or normal?
        //var chosenSweep = Math.random() > 0.5f ? sweep : sweepReverse;
        var chosenSweep = (slot == EquipmentSlot.MAINHAND) ? sweep : sweepReverse;
        lastUsedSlot = slot;

        // actually spawn sweep attack particle
        sweepParticle = pe.createParticle(chosenSweep,
            player.position().x(), // spawn it inside player first, so lighting matches up
            player.position().y()+player.getBbHeight()/2, // otherwise, particle can spawn inside blocks, making it look too dark
            player.position().z(),
            0.0, 0.0, 0.0);

        if (sweepParticle != null) {
            sweepParticle.setColor(0.75f, 0.75f, 0.75f);
            sweepParticle.setLifetime(lifetime);
        }

        // if weapon has fire aspect, make it ORANGE
        ItemStack stack = player.getItemBySlot(slot);
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
            sweepParticle.setLifetime(lifetime);

        }

        // if crit is possible, turn particle red
        if (!player.onGround() && player.getDeltaMovement().y < -0.15) 
            sweepParticle.setColor(0.6f, 0.1f, 0.1f);

        // set angle of particle
        if (sweepParticle instanceof CustomSweepParticle) {
            ((CustomSweepParticle)sweepParticle).setAngle(0);
        }
    }

    public static List<Entity> getEntitiesInSweepCone(Player player, double reach) {
        double normalizedReach = Mth.clamp((reach - 1.0) / (6.0 - 1.0), 0.0, 1.0);
        double radius = Mth.lerp(1.0 - normalizedReach, minSweep, maxSweep);

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
                // config stuff check
                boolean hostile = entity instanceof Enemy || (entity instanceof Mob mob && mob.getTarget() == player);
                if (FluidCombat.CONFIG.get(ServerConfig.class).sweepOnlyHitsHostiles && !hostile) return false;

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
            int baseLimit = 3;
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

    public static void drawSweepTube(Player player, double reach) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        double normalizedReach = Mth.clamp((reach - 1.0) / (6.0 - 1.0), 0.0, 1.0);
        double radius = Mth.lerp(1.0 - normalizedReach, minSweep, maxSweep);

        int steps = 20;
        double stepSize = reach / steps;

        // particle color (unchanged behavior)
        var particleColor = new Vector3f(0.75f, 0.75f, 0.75f);
        if (!player.onGround() && player.getDeltaMovement().y < -0.15) {
            particleColor = new Vector3f(0.6f, 0.1f, 0.1f);
        }

        var particle = new DustParticleOptions(particleColor, 0.25F);

        // build orientation frame
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(look.dot(up)) > 0.99) up = new Vec3(1, 0, 0);

        Vec3 right = look.cross(up).normalize();
        Vec3 perpUp = right.cross(look).normalize();

        // rotation (shared by tube + cross)
        double rotation = player.tickCount * 0.05;

        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);

        Vec3 r = right.scale(cos).add(perpUp.scale(sin));
        Vec3 u = right.scale(-sin).add(perpUp.scale(cos));

        for (int i = 0; i <= steps; i++) {
            Vec3 point = eye.add(look.scale(i * stepSize));
            drawCircleParticles(serverLevel, particle, point, r, u, radius, 8);
        }

        Vec3 end = eye.add(look.scale(reach));
        drawEndCross(serverLevel, particle, end, r, u, radius, 8);
    }

    private static void drawCircleParticles(ServerLevel level, DustParticleOptions particle, Vec3 center, Vec3 right, Vec3 up, double radius, int segments) {
        for (int i = 0; i < segments; i++) {
            double angle = (2 * Math.PI / segments) * i;
            Vec3 offset = right.scale(Math.cos(angle))
                    .add(up.scale(Math.sin(angle)))
                    .scale(radius);
            Vec3 particlePos = center.add(offset);
            level.sendParticles(
                    particle,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    0, 0, 0,
                    2
            );
        }
    }

    private static void drawEndCross(ServerLevel level, DustParticleOptions particle, Vec3 center, Vec3 right, Vec3 up, double radius, int segments) {
        Vec3[] dirs = new Vec3[]{
                right,
                up,
                right.add(up).normalize(),
                right.subtract(up).normalize()
        };

        for (Vec3 dir : dirs) {
            for (int i = -segments; i <= segments; i++) {
                double t = (double) i / segments;
                Vec3 pos = center.add(dir.scale(radius * t));
                level.sendParticles(
                        particle,
                        pos.x,
                        pos.y,
                        pos.z,
                        1,
                        0, 0, 0,
                        0
                );
            }
        }
    }

    public static void updateSweepAttackParticle(Particle sweepParticle, Player player) {
        if (sweepParticle == null) return;

        float sidewaysOffset = (lastUsedSlot == EquipmentSlot.MAINHAND) ? SweepAttackHelper.sidewaysOffset : -SweepAttackHelper.sidewaysOffset;

        Vec3 newPos = null;
        if (sweepParticle == secondarySweepParticle) {
            newPos = getSweepAttackPosition(player, forwardOffset-0.05f, downwardOffset, sidewaysOffset);
        } else {
            newPos = getSweepAttackPosition(player, forwardOffset, downwardOffset, sidewaysOffset);
        }
        sweepParticle.setPos(newPos.x(), newPos.y(), newPos.z());
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

    public static boolean canSweepAttack(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) return false;

        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.isEmpty()) return false;

        return modifiers.containsKey(Attributes.ATTACK_DAMAGE);
    }

    // currently unused...
    public static void previewSweepTargets(Player player, EquipmentSlot slot) {
        Level level = player.level();
        if (level == null) return;

        if (!canSweepAttack(player, slot)) return;

        // only show preview when attack is almost ready
        //if (player.getAttackStrengthScale(0) < 0.9f) return;

        double reach = player.getAttributeValue(CommonAbstractions.INSTANCE.getAttackRangeAttribute());

        List<Entity> targets = getEntitiesInSweepCone(player, reach);

        for (Entity entity : targets) {

            double x = entity.getX();
            double y = entity.getY() + entity.getEyeHeight() + 0.5;
            double z = entity.getZ();

            // spawn a single visible particle
             level.addParticle(
                    ParticleTypes.CRIT,
                    x, y, z,
                    0.0, 0.0, 0.0
            );
        }
    }
}