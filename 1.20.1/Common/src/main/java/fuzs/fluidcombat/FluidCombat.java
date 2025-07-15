package fuzs.fluidcombat;

import fuzs.fluidcombat.config.ClientConfig;
import fuzs.fluidcombat.config.ServerConfig;
import fuzs.fluidcombat.handler.ClassicCombatHandler;
import fuzs.fluidcombat.handler.CombatTestHandler;
import fuzs.fluidcombat.network.client.ServerboundSweepAttackMessage;
import fuzs.fluidcombat.network.client.ServerboundSwingArmMessage;
import fuzs.fluidcombat.particles.ModParticles;
import fuzs.puzzleslib.api.config.v3.ConfigHolder;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.event.v1.entity.ProjectileImpactCallback;
import fuzs.puzzleslib.api.event.v1.entity.living.LivingHurtCallback;
import fuzs.puzzleslib.api.event.v1.entity.living.LivingKnockBackCallback;
import fuzs.puzzleslib.api.event.v1.entity.living.UseItemEvents;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerInteractEvents;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerTickEvents;
import fuzs.puzzleslib.api.network.v3.NetworkHandlerV3;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FluidCombat implements ModConstructor {
    public static final String MOD_ID = "fluidcombat";
    public static final String MOD_NAME = "Fluid Combat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static final NetworkHandlerV3 NETWORK = NetworkHandlerV3.builder(MOD_ID).registerServerbound(ServerboundSweepAttackMessage.class).registerServerbound(ServerboundSwingArmMessage.class);
    public static final ConfigHolder CONFIG = ConfigHolder.builder(MOD_ID).client(ClientConfig.class).server(ServerConfig.class);

    @Override
    public void onConstructMod() {
        registerHandlers();
        //registerParticles();
    }

    private static void registerParticles() {
        ModParticles.register();
    }

    private static void registerHandlers() {
        //ItemAttributeModifiersCallback.EVENT.register(AttackAttributeHandler::onItemAttributeModifiers);
        LivingKnockBackCallback.EVENT.register(ClassicCombatHandler::onLivingKnockBack);
        ProjectileImpactCallback.EVENT.register(ClassicCombatHandler::onProjectileImpact);
        PlayerInteractEvents.USE_ITEM.register(CombatTestHandler::onUseItem);
        UseItemEvents.START.register(CombatTestHandler::onUseItemStart);
        PlayerTickEvents.START.register(CombatTestHandler::onStartPlayerTick);
        LivingHurtCallback.EVENT.register(CombatTestHandler::onLivingHurt);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}