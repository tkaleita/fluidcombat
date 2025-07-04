package fuzs.combatnouveau.helper;

import fuzs.combatnouveau.core.CommonAbstractions;
import fuzs.combatnouveau.particles.CustomSweepParticle;
import fuzs.combatnouveau.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SweepAttackHelper {

    public static Particle sweepParticle;

    public static void initiateSweepAttack(Player player) {
        float attackDamage = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        if (attackDamage > 0.0F && allowSweepAttack(player)) {
            double moveX = (double) (-Mth.sin(player.getYRot() * ((float) Math.PI / 180))) * 2.0;
            double moveZ = (double) Mth.cos(player.getYRot() * ((float) Math.PI / 180)) * 2.0;
            AABB aABB = CommonAbstractions.INSTANCE.getSweepHitBox(player, player).move(moveX, 0.0, moveZ);
            sweepAttack(player, aABB, attackDamage, null);
        }
        // also resets attack ticker
        player.swing(InteractionHand.MAIN_HAND);
    }

    private static boolean allowSweepAttack(Player player) {
        return true;
        /*if (CombatNouveau.CONFIG.get(ServerConfig.class).noSweepingWhenSneaking && player.isShiftKeyDown()) {
            return false;
        } else if (CombatNouveau.CONFIG.get(ServerConfig.class).requireSweepingEdge) {
            return player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
        } else {
            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            return ToolTypeHelper.INSTANCE.isSword(itemInHand);
        }*/
    }

    private static void sweepAttack(Player player, AABB aABB, float attackDamage, @Nullable Entity target) {
        float sweepingAttackDamage = 1.0F + (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage;
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, aABB);

        list.removeIf(x ->
            x == player ||
            x == target ||
            player.isAlliedTo(x) ||
            (x instanceof ArmorStand && ((ArmorStand) x).isMarker()) ||
            (target != null && target.distanceToSqr(x) >= 9.0)); // maybe make it so the higher the range, the less sweep range a weapon has?

        for (LivingEntity livingEntity : list) {
            DamageSource damageSource = player.damageSources().playerAttack(player);
            float enchantedDamage = player.getEnchantedDamage(livingEntity, sweepingAttackDamage, damageSource);
            livingEntity.knockback(
                    0.4F, (double)Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)), (double)(-Mth.cos(player.getYRot() * (float) (Math.PI / 180.0)))
            );
            livingEntity.hurt(damageSource, enchantedDamage);
            if (player.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, livingEntity, damageSource);
            }
        }
    }

    public static void spawnSweepAttackEffects(Player player, Level level) {
        if (!canSweepAttack(player)) return;
        Minecraft mc = Minecraft.getInstance();
        ParticleEngine pe = mc.particleEngine;
        var sweepParticlePositon = SweepAttackHelper.getSweepAttackPosition(player, 1.15, 0.5);
        sweepParticle = pe.createParticle(ModParticles.CUSTOM_SWEEP,
            sweepParticlePositon.x(),
            sweepParticlePositon.y(),
            sweepParticlePositon.z(),
            0.0, 0.0, 0.0);
        if (!player.onGround() && player.getDeltaMovement().y < 0.0) // if crit is possible, turn particle red
            sweepParticle.setColor(0.6f, 0.1f, 0.1f);
        if (sweepParticle instanceof CustomSweepParticle) { // set angle of particle
            ((CustomSweepParticle)sweepParticle).setAngle(0);
        }
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

    public static boolean updateSweepAttackParticle(Player player, Level level) {
        if (sweepParticle == null) return false;

        var newPos = getSweepAttackPosition(player, 1.15, 0.5);
        sweepParticle.setPos(newPos.x(), newPos.y(), newPos.z());
        return true;
    }

    private static boolean canSweepAttack(Player player) {
        ItemStack stack = player.getMainHandItem();
        var components = stack.getComponents();

        if (!components.has(DataComponents.ATTRIBUTE_MODIFIERS)) return false;

        var modifiers = components.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;

        return modifiers.modifiers().stream().anyMatch(x -> x.attribute() == Attributes.ATTACK_DAMAGE);
    }

    public static Vec3 getSweepAttackPosition(Player player, double forwardOffset, double downwardOffset) {

        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle().normalize();

        // The world's up vector (in Minecraft: Y+) 
        Vec3 upVector = new Vec3(0.0, 1.0, 0.0);

        // Right vector = look × up (cross product)
        Vec3 rightVector = lookVector.cross(upVector).normalize();

        // Down vector (on screen) = right × look
        Vec3 downScreenVector = rightVector.cross(lookVector).normalize();

        // Forward position
        Vec3 forwardPosition = eyePosition.add(lookVector.scale(forwardOffset));

        // Now move it downward relative to screen orientation
        Vec3 finalPosition = forwardPosition.add(downScreenVector.scale(-downwardOffset));

        return finalPosition;
    }
}
