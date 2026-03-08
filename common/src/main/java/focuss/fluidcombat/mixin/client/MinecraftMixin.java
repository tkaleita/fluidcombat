package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.config.ClientConfig;
import focuss.fluidcombat.config.ServerConfig;
import focuss.fluidcombat.helper.FluidCombatHelper;
import focuss.fluidcombat.mixin.client.accessor.LivingEntityAccessor;
import focuss.fluidcombat.mixin.client.accessor.MultiPlayerGameModeAccessor;
import focuss.fluidcombat.network.client.ServerboundBlockCritEffectsMessage;
import focuss.fluidcombat.network.client.ServerboundBreakBlockMessage;
import focuss.fluidcombat.network.client.ServerboundSweepAttackMessage;
import focuss.fluidcombat.platform.Services;
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
import net.minecraft.world.entity.player.Player;
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
    ItemStack fluidCombat$lastAttackStack = ItemStack.EMPTY;
    @Unique
    private boolean fluidCombat$nextAttackOffhand = false;
    @Unique
    private int fluidCombat$lastAttackTick = 0;
    @Unique
    private ItemStack fluidCombat$lastHeldItem = ItemStack.EMPTY;
    @Unique
    private int fluidCombat$lastHotbarSlot = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void fluidcombat$resetAlternatingAttacks(CallbackInfo ci) {
        if (player == null || player.tickCount % 5 == 0) return;

        ItemStack current = player.getMainHandItem();
        int currentSlot = player.getInventory().selected;

        // 3 ways: reset through timeout, item in main hand changed, or selected hotbar slot changed
        if (player.tickCount - fluidCombat$lastAttackTick > 40
                || !ItemStack.isSameItem(current, fluidCombat$lastHeldItem)
                || currentSlot != fluidCombat$lastHotbarSlot) {
            fluidCombat$nextAttackOffhand = false;
        }
        fluidCombat$lastHeldItem = current.copy();
        fluidCombat$lastHotbarSlot = player.getInventory().selected;
    }

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
        // disable right click when we can alternate attacks. only left click is needed then
        if (fluidCombat$canAlternate()) return;

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
        float cooldown = FluidCombatHelper.getCooldownTicks(player, last);
        return ticker >= cooldown;
    }

    @Unique
    private boolean fluidCombat$canAlternate() {
        if (!FluidCombat.CONFIG.get(ClientConfig.class).alternatingAttacks) return false;

        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();

        if (main.isEmpty() || off.isEmpty()) return false;
        if (!fluidCombat$isWeapon(EquipmentSlot.MAINHAND)) return false;
        if (!fluidCombat$isWeapon(EquipmentSlot.OFFHAND)) return false;
        if (!Services.PLATFORM.canUseItemClient(player, player.getItemBySlot(EquipmentSlot.OFFHAND))) return false;
        if (FluidCombat.CONFIG.get(ClientConfig.class).unrestrictedAlternatingAttacks) return true;

        return ItemStack.isSameItem(main, off);
    }

    @Unique
    private void fluidCombat$triggerAttack(EquipmentSlot slot, boolean damageBlock) {
        //FluidCombat.LOGGER.info("can attack using new cd: {}", fluidCombat$canAttack());
        if (!fluidCombat$canAttack()) {
            return;
        }
        if (fluidCombat$canAlternate()) {
            slot = fluidCombat$nextAttackOffhand ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            fluidCombat$nextAttackOffhand = !fluidCombat$nextAttackOffhand;
        }
        // can we use this item? if leveling mods are installed
        if (!Services.PLATFORM.canUseItemClient(player, player.getItemBySlot(slot))) {
            return;
        }

        FluidCombatHelper.lastUsedSlot = slot;
        fluidCombat$lastAttackStack = player.getItemBySlot(slot).copy();
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        accessor.fluidCombat$setAttackStrengthTicker(0);

        //FluidCombat.LOGGER.info("calculated CD: {}", fluidCombat$getCooldownTicks(player, fluidCombat$lastAttackStack));
        fluidCombat$lastAttackTick = player.tickCount;
        fluidCombat$triggerSweep(slot);
        if (damageBlock)
            fluidCombat$damageBlock(slot, Minecraft.getInstance().hitResult);
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
        ItemStack stack = player.getItemBySlot(slot);

        // determine destroy speed & cooldown using the correct stack
        float speed;
        if (slot == EquipmentSlot.MAINHAND) {
            speed = player.getDestroySpeed(state);
            speed = Services.PLATFORM.modifyBreakSpeed(player, state, pos, speed);
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

        // get damage
        var damage = fluidCombat$calculateBlockDamage(slot, hit, speed);
        fluidCombat$damage += damage;
        FluidCombat.LOGGER.info("damage: {}", damage);

        // cd advance
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        var cooldownAdvance = fluidCombat$calculateCooldownAdvance(player, slot, hit, damage, speed);
        accessor.fluidCombat$setAttackStrengthTicker((int)cooldownAdvance);
        FluidCombat.LOGGER.info("cooldown advance: {}", cooldownAdvance);

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
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 0.0, 0.0, 0.0
            );
        }
    }

    @Unique
    private float fluidCombat$calculateBlockDamage(EquipmentSlot slot, HitResult hit, float speed) {
        if (player.isCreative()) return 100;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = player.getItemBySlot(slot);

        // determine how much damage this hit deals
        boolean canMineBlock = Services.PLATFORM.canMineBlock(player, stack, state, pos);
        float divisor = canMineBlock ? 30f : 100f;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= -1) return 0;
        float damagePerTick = speed / hardness / divisor;
        float cooldownTicks = FluidCombatHelper.getCooldownTicks(player, stack);
        float damage = (damagePerTick * cooldownTicks)*1.01f; // just a lil buff for cases like 0.333
        // are we critting? if so, increase damage and spawn crit sfx
        if (FluidCombatHelper.isPlayerCritting(player, true)) {
            damage *= 1.5f;
            FluidCombat.NETWORK.sendToServer(new ServerboundBlockCritEffectsMessage(pos.getX(), pos.getY(), pos.getZ()));
        }
        return damage;
    }

    @Unique
    private float fluidCombat$calculateCooldownAdvance(Player player, EquipmentSlot slot, HitResult hit, float damage, float speed) {
        ItemStack stack = player.getItemBySlot(slot);
        float cooldownTicks = FluidCombatHelper.getCooldownTicks(player, stack);

        if (player.isCreative()) return cooldownTicks-4; // n tick delay between destroying blocks when in creative

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = level.getBlockState(pos);

        // determine how many swings the block should take
        float swingsNeeded = (float) Math.ceil(1f / damage);

        // how much damage the last swing overshoots
        float totalDamage = swingsNeeded * damage;
        float excessDamage = totalDamage - 1f;

        if (excessDamage <= 0f) return 0f;

        // distribute excess across swings
        float excessPerSwing = excessDamage / totalDamage;

        // convert excess damage into cooldown ticks
        float cooldownAdvance = excessPerSwing * cooldownTicks * 0.9f; // small nerf to make it harder to hit cap

        // clamp to reasonable bounds
        cooldownAdvance = Mth.clamp(cooldownAdvance, 0f, cooldownTicks);

        // ensure we never exceed vanilla break speed
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness > 0f) {
            boolean correctTool = Services.PLATFORM.canMineBlock(player, stack, state, pos);

            // replicating vanilla calculation so we use normal divisor
            float divisor = correctTool ? 30f : 100f;
            float progressPerTick = speed / hardness / divisor;
            int vanillaTicksToBreak = (int) Math.ceil(1f / progressPerTick);

            // insta mine soft gate
            vanillaTicksToBreak = Math.max(vanillaTicksToBreak, 2); // clamp to 2 to account for minecraft vanilla mining cooldown

            float maxAdvance = cooldownTicks - vanillaTicksToBreak;
            if (maxAdvance < 0f) maxAdvance = cooldownTicks - maxAdvance;

            cooldownAdvance = Math.min(cooldownAdvance, maxAdvance);
        } else if (hardness == 0.0f) {
            // 0 hardness block, like grass, flower, torch, crops, etc
            return cooldownTicks-5; // 5 cooldown for 0 hardness blocks
        }

        FluidCombat.LOGGER.info(String.valueOf(cooldownAdvance));
        cooldownAdvance = (float) Math.round(cooldownAdvance);
        FluidCombat.LOGGER.info(String.valueOf(cooldownAdvance));

        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        return Math.min(accessor.fluidcombat$getAttackStrengthTicker() + (int) cooldownAdvance, (int) cooldownTicks);
    }

    @Unique
    private void fluidCombat$triggerSweep(EquipmentSlot slot) {
        // check damage of current item
        if (fluidCombat$isWeapon(slot)) { // do all effects if current item has damage attribute!
            float damage = FluidCombatHelper.getItemAttackDamage(player.getItemBySlot(slot));
            FluidCombatHelper.spawnSweepAttackEffects(player, level, slot, damage, (int) FluidCombatHelper.getCooldownTicks(player, player.getItemBySlot(slot)));
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