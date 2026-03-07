package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.config.ServerConfig;
import focuss.fluidcombat.helper.SweepAttackHelper;
import focuss.fluidcombat.mixin.client.accessor.LivingEntityAccessor;
import focuss.fluidcombat.mixin.client.accessor.MultiPlayerGameModeAccessor;
import focuss.fluidcombat.network.client.ServerboundBlockCritEffectsMessage;
import focuss.fluidcombat.network.client.ServerboundBreakBlockMessage;
import focuss.fluidcombat.network.client.ServerboundSweepAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.SoundType;
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
    private BlockPos fluidCombat$damagePos;
    @Unique
    private float fluidCombat$damage;
    @Unique
    private ItemStack fluidCombat$lastAttackStack = ItemStack.EMPTY;

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
            if (attacking && !this.player.isUsingItem() && fluidCombat$canAttack()) {
                //FluidCombat.LOGGER.info("damageBlock from continueAttack");
                fluidCombat$triggerAttack(EquipmentSlot.MAINHAND, true);
            }
            callback.cancel();
            return;
        }

        if (attacking && !this.player.isUsingItem() && (hit == null || hit.getType() != HitResult.Type.BLOCK)) {
            if (fluidCombat$canAttack()) {
                this.startAttack();
            }
        }
    }

    @Inject(method = "startAttack", at = @At(value = "HEAD"), cancellable = true)
    private void startAttackHead(CallbackInfoReturnable<Boolean> cir) {
        //FluidCombat.LOGGER.info("startAttackHead");

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            // we are trying to mine a block! cancel!! (unless continueAttack is disabled)
            if (fluidCombat$canAttack() && !FluidCombat.CONFIG.get(ServerConfig.class).holdAttackButton) {
                fluidCombat$triggerAttack(EquipmentSlot.MAINHAND, true);
            }
            cir.setReturnValue(false);
            return;
        }

        if (!fluidCombat$canAttack()) {
            // block the vanilla attack altogether
            cir.setReturnValue(false);
        }
    }

    /// because the sweep resets attack cooldown, this NEEDS to be injected at return. to let minecraft handle the normal attack!
    @Inject(method = "startAttack", at = @At(value = "RETURN"))//, target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V", shift = At.Shift.BEFORE), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> callback) {
        //FluidCombat.LOGGER.info("startAttackReturn");
        fluidCombat$triggerAttack(EquipmentSlot.MAINHAND, false);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void fluidCombat$offhandAttack(CallbackInfo ci) {
        if (player == null || level == null) return;
        if (!fluidCombat$isWeapon(EquipmentSlot.OFFHAND)) return;

        // if we can interact with the block were targeting, dont attack and interact instead
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockState state = level.getBlockState(blockHit.getBlockPos());
            if (state.use(level, player, InteractionHand.MAIN_HAND, blockHit).consumesAction()) {
                return;
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        Item item = mainHand.getItem();

        boolean mainhandIsWeapon = fluidCombat$isWeapon(EquipmentSlot.MAINHAND);
        boolean mainhandHasUse =
                    mainHand.isEdible()
                    || mainHand.getUseAnimation() != UseAnim.NONE
                    || mainHand.getUseDuration() > 0
                    || item instanceof SpawnEggItem
                    || item instanceof PotionItem
                    || item instanceof BlockItem
                    || item instanceof EnderpearlItem
                    || item instanceof EnderEyeItem
                    || item instanceof BucketItem
                    || item instanceof ShearsItem;

        if (!mainhandIsWeapon && mainhandHasUse) return;

        fluidCombat$triggerAttack(EquipmentSlot.OFFHAND, true);
        ci.cancel();
    }

    @Unique
    private boolean fluidCombat$canAttack() {
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        int ticker = accessor.fluidcombat$getAttackStrengthTicker();
        ItemStack last = fluidCombat$lastAttackStack;
        float cooldown = fluidCombat$getCooldownTicks(player, last);
        return ticker >= cooldown;
    }

    @Unique
    private void fluidCombat$triggerAttack(EquipmentSlot slot, boolean damageBlock) {
        //FluidCombat.LOGGER.info("can attack using new cd: {}", fluidCombat$canAttack());
        if (!fluidCombat$canAttack()) {
            return;
        }

        fluidCombat$lastAttackStack = player.getItemBySlot(slot).copy();
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        accessor.fluidcombat$setAttackStrengthTicker(0);

        //FluidCombat.LOGGER.info("calculated CD: {}", fluidCombat$getCooldownTicks(player, fluidCombat$lastAttackStack));

        fluidCombat$triggerSweep(slot);
        if (damageBlock)
            fluidCombat$damageBlock(slot, Minecraft.getInstance().hitResult);
    }

    @Unique
    private float fluidCombat$getCooldownTicks(LocalPlayer player, ItemStack stack) {
        if (stack == null) return 0;

        var attributes = player.getAttributes();

        ItemStack main = player.getMainHandItem();

        var mainMods = main.getAttributeModifiers(EquipmentSlot.MAINHAND);
        var stackMods = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

        try {
            attributes.removeAttributeModifiers(mainMods);
            attributes.addTransientAttributeModifiers(stackMods);

            double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
            if (attackSpeed <= 0) attackSpeed = 0.1;

            return (float)(20.0 / attackSpeed);

        } finally {
            attributes.removeAttributeModifiers(stackMods);
            attributes.addTransientAttributeModifiers(mainMods);
        }
    }

    @Unique
    private boolean fluidCombat$isWeapon(EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
    }

    @Unique
    private void fluidCombat$damageBlock(EquipmentSlot slot, HitResult hit) {
        if (this.gameMode == null || this.level == null || hit == null || hit.getType() != HitResult.Type.BLOCK) return;
        //FluidCombat.LOGGER.info("damageBlock with: {}", player.getItemBySlot(slot));

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = level.getBlockState(pos);

        // determine destroy speed & cooldown using the correct stack
        ItemStack stack = player.getItemBySlot(slot);
        float speed;
        if (slot == EquipmentSlot.MAINHAND) {
            speed = player.getDestroySpeed(state);
        } else {
            ItemStack original = player.getMainHandItem();

            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
            speed = player.getDestroySpeed(state);
            player.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
        // undo vanilla mining in air penalty
        if (!player.onGround()) {
            speed *= 5.0F;
        }

        // reset progress if we changed block
        if (!pos.equals(fluidCombat$damagePos)) {
            fluidCombat$damagePos = pos;
            fluidCombat$damage = 0f;
        }

        // determine how much damage this hit deals
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0) return;
        float damagePerTick = speed / hardness / 40f; // maybe add correct tool scaling to this instead of a flat number.
        float cooldownTicks = fluidCombat$getCooldownTicks(player, stack);
        float damage = (damagePerTick * cooldownTicks)*1.01f; // just a lil buff for cases like 0.333
        //FluidCombat.LOGGER.info(String.valueOf(damage));
        if (player.isCreative())
            damage = 100;
        // are we critting? if so, increase damage and spawn crit sfx
        if (SweepAttackHelper.isPlayerCritting(player)) {
            damage *= 1.5f;
            FluidCombat.NETWORK.sendToServer(new ServerboundBlockCritEffectsMessage(pos.getX(), pos.getY(), pos.getZ()));
        }
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
            //FluidCombat.LOGGER.info(String.valueOf(ticker));
            if (player.isCreative())
                accessor.fluidcombat$setAttackStrengthTicker(100);

            //FluidCombat.LOGGER.info("smooth CD advance: {}", cooldownAdvance);
        }

        // play block hit sound
        if (damage >= 0.5f) {
            // strong hit
            level.playSound(player, pos, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.35F, 0.9F + level.random.nextFloat() * 0.2F);
        } else {
            // weak hit
            level.playSound(player, pos, SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.35F, 1.0F + level.random.nextFloat() * 0.2F);
        }

        // play block mining sound
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
            FluidCombat.NETWORK.sendToServer(new ServerboundBreakBlockMessage(slot, pos.getX(), pos.getY(), pos.getZ()));
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
    private void fluidCombat$triggerSweep(EquipmentSlot slot) {
        // check damage of current item
        if (fluidCombat$isWeapon(slot)) { // do all effects if current item has damage attribute!
            float damage = SweepAttackHelper.getItemAttackDamage(player.getItemBySlot(slot));
            SweepAttackHelper.spawnSweepAttackEffects(player, level, slot, damage, (int)fluidCombat$getCooldownTicks(player, player.getItemBySlot(slot)));
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

        //this.player.resetAttackStrengthTicker();
        player.swing(slot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

        // if we cant actually sweep. dont send the info to the server
        if (player.isCrouching() && FluidCombat.CONFIG.get(ServerConfig.class).noSweepingWhenSneaking)
            return;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, player.getItemBySlot(slot)) <= 0 && FluidCombat.CONFIG.get(ServerConfig.class).requireSweepingEdge)
            return;

        ((MultiPlayerGameModeAccessor) this.gameMode).fluidcombat$callEnsureHasSentCarriedItem();
        FluidCombat.NETWORK.sendToServer(new ServerboundSweepAttackMessage((this.player).isShiftKeyDown(), slot));
    }
}