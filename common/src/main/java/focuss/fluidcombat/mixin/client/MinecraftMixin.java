package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.client.handler.AutoAttackHandler;
import focuss.fluidcombat.config.ServerConfig;
import focuss.fluidcombat.helper.SweepAttackHelper;
import focuss.fluidcombat.mixin.client.accessor.LivingEntityAccessor;
import focuss.fluidcombat.mixin.client.accessor.MultiPlayerGameModeAccessor;
import focuss.fluidcombat.network.client.ServerboundBreakBlockMessage;
import focuss.fluidcombat.network.client.ServerboundSweepAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow
    public ClientLevel level;
    @Shadow
    public LocalPlayer player;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Shadow
    private boolean startAttack() {
        throw new RuntimeException();
    }
    @Unique
    boolean fluidCombat$wasCharged = false;
    @Unique
    private BlockPos fluidCombat$damagePos;
    @Unique
    private float fluidCombat$damage;

    @Inject(method = "continueAttack", at = @At(value = "HEAD"), cancellable = true)//, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V", shift = At.Shift.BEFORE))
    private void continueAttack(boolean attacking, CallbackInfo callback) {
        if (!FluidCombat.CONFIG.get(ServerConfig.class).holdAttackButton) {
            callback.cancel();
            return;
        }
        //FluidCombat.LOGGER.info("continueAttack");

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            // we are trying to mine a block!
            if (attacking && !this.player.isUsingItem() && AutoAttackHandler.readyForAutoAttack(this.player)) {
                //FluidCombat.LOGGER.info("damageBlock from continueAttack");
                fluidCombat$triggerSweep();
                fluidCombat$damageBlock((BlockHitResult)hit);
            }
            callback.cancel();
            return;
        }

        if (attacking && !this.player.isUsingItem() && (hit == null || hit.getType() != HitResult.Type.BLOCK)) {
            if (AutoAttackHandler.readyForAutoAttack(this.player)) {
                this.startAttack();
            }
        }
    }

    @Inject(method = "startAttack", at = @At(value = "HEAD"), cancellable = true)
    private void startAttackHead(CallbackInfoReturnable<Boolean> cir) {
        //FluidCombat.LOGGER.info("startAttackHead");

        fluidCombat$wasCharged = player.getAttackStrengthScale(0.5F) >= 1.0F;
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            // we are trying to mine a block! cancel!! (unless continueAttack is disabled)
            if (fluidCombat$wasCharged && !FluidCombat.CONFIG.get(ServerConfig.class).holdAttackButton) {
                fluidCombat$triggerSweep();
                fluidCombat$damageBlock((BlockHitResult)hit);
            }
            cir.setReturnValue(false);
            return;
        }

        if (!fluidCombat$wasCharged) {
            // block the vanilla attack altogether
            cir.setReturnValue(false);
        }
    }

    /// because the sweep resets attack cooldown, this NEEDS to be injected at return. to let minecraft handle the normal attack!
    @Inject(method = "startAttack", at = @At(value = "RETURN"))//, target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V", shift = At.Shift.BEFORE), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> callback) {
        //FluidCombat.LOGGER.info("startAttackReturn");

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) return;

        if (!fluidCombat$wasCharged) return;
        fluidCombat$triggerSweep();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void fluidCombat$offhandAttack(CallbackInfo ci) {
        if (player == null || level == null) return;
        ItemStack offhand = player.getOffhandItem();
        if (!fluidCombat$isWeapon(offhand)) return;
        if (player.getAttackStrengthScale(0.5F) < 1.0F) {
            ci.cancel();
            return;
        }
        fluidCombat$triggerSweepOffhand();
        ci.cancel();
    }

    @Unique
    private boolean fluidCombat$isWeapon(ItemStack stack) {
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.OFFHAND);
        for (AttributeModifier mod : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getAmount() > 0) return true;
        }
        return false;
    }

    @Unique
    private void fluidCombat$triggerSweepOffhand() {
        SweepAttackHelper.spawnSweepAttackEffects(player, level);
        player.swing(InteractionHand.OFF_HAND, true);
        player.resetAttackStrengthTicker();
        FluidCombat.NETWORK.sendToServer(
                new ServerboundSweepAttackMessage(true) // mark offhand
        );
    }

    @Unique
    private void fluidCombat$damageBlock(BlockHitResult hit) {
        if (this.gameMode == null || this.level == null) return;
        FluidCombat.LOGGER.info("damageBlock");

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.getDestroySpeed(level, pos) < 0) return;
        // reset progress if we changed block
        if (!pos.equals(fluidCombat$damagePos)) {
            fluidCombat$damagePos = pos;
            fluidCombat$damage = 0f;
        }

        // determine how much damage this hit deals
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0) return;
        float speed = player.getDestroySpeed(state);
        float damagePerTick = speed / hardness / 40f; // maybe add correct tool scaling to this instead of a flat number.
        float cooldownTicks = player.getCurrentItemAttackStrengthDelay();
        float damage = (damagePerTick * cooldownTicks)*1.01f; // just a lil buff for cases like 0.333
        FluidCombat.LOGGER.info(String.valueOf(damage));
        if (player.isCreative())
            damage = 100;
        fluidCombat$damage += damage;

        // cooldown advance for strong tools
        float swingsNeeded = (float)Math.ceil(1f / damage);
        float excessPerSwing = (swingsNeeded * damage - 1f) / swingsNeeded;
        if (excessPerSwing > 0f) {
            float cooldownAdvance = excessPerSwing * cooldownTicks;

            if (cooldownAdvance < 0f) cooldownAdvance = 0f;
            if (cooldownAdvance > cooldownTicks) cooldownAdvance = cooldownTicks; // max cd

            // cap it to max vanilla speed
            float progressPerTick = speed / (hardness * 75f);
            int vanillaTicksToBreak = (int)Math.ceil(1f / progressPerTick);
            float maxAdvance = cooldownTicks - vanillaTicksToBreak;
            if (maxAdvance < 0f) maxAdvance = 0f;
            cooldownAdvance = Math.min(cooldownAdvance, maxAdvance);

            LivingEntityAccessor accessor = (LivingEntityAccessor)player;
            int ticker = accessor.fluidcombat$getAttackStrengthTicker();
            ticker = Math.min(ticker + (int)cooldownAdvance, (int)cooldownTicks);
            accessor.fluidcombat$setAttackStrengthTicker(ticker);
            if (player.isCreative())
                accessor.fluidcombat$setAttackStrengthTicker(100);

            FluidCombat.LOGGER.info("smooth CD advance: {}", cooldownAdvance);
        }

        // play sound
        SoundType sound = state.getSoundType();
        level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                sound.getHitSound(),
                SoundSource.BLOCKS,
                Mth.lerp(fluidCombat$damage, 0.25f, 0.5f) * sound.getVolume(),
                sound.getPitch(),
                false
        );

        // update block damage progress
        int stage = (int)(fluidCombat$damage * 10f);
        level.destroyBlockProgress(player.getId(), pos, stage);

        if (fluidCombat$damage >= 1.0f) {
            FluidCombat.NETWORK.sendToServer(new ServerboundBreakBlockMessage(pos.getX(), pos.getY(), pos.getZ()));
            level.destroyBlockProgress(player.getId(), pos, -1);
            fluidCombat$damage = 0f;
            fluidCombat$damagePos = null;
            return;
        }

        // block hit particle effects if block isnt destroyed
        for (int i = 0; i < 50*damage; i++) {
            double x = pos.getX() + level.random.nextDouble();
            double y = pos.getY() + level.random.nextDouble();
            double z = pos.getZ() + level.random.nextDouble();
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, y, z,
                    0.0, 0.0, 0.0
            );
        }
    }

    @Unique
    private void fluidCombat$triggerSweep() {
        // check damage of current item
        ItemStack stack = player.getMainHandItem();
        float weaponDamage = 0f;
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier mod : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            weaponDamage += (float) mod.getAmount();
        }
        FluidCombat.LOGGER.info("Weapon damage: {}", weaponDamage);

        if (weaponDamage > 0.0F) { // do all effects if current item has damage attribute!
            SweepAttackHelper.spawnSweepAttackEffects(player, level);
        } else {
            // play a small, quieter version of the sound
            float pitch = 2f + RandomSource.create().nextFloat() * 0.1F; // random pitch +-0.1f i think
            level.playSound(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    0.1f, // volume
                    pitch
            );
        }

        this.player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND, true); // force full swing animation

        // if we cant actually sweep. dont send the info to the server
        if (player.isCrouching() && FluidCombat.CONFIG.get(ServerConfig.class).noSweepingWhenSneaking)
            return;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, player.getMainHandItem()) <= 0 && FluidCombat.CONFIG.get(ServerConfig.class).requireSweepingEdge)
            return;

        ((MultiPlayerGameModeAccessor) this.gameMode).fluidcombat$callEnsureHasSentCarriedItem();
        FluidCombat.NETWORK.sendToServer(new ServerboundSweepAttackMessage((this.player).isShiftKeyDown()));
    }
}