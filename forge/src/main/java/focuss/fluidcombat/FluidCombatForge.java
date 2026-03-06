package focuss.fluidcombat;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.client.FluidCombatForgeClient;
import focuss.fluidcombat.core.CommonAbstractions;
import focuss.fluidcombat.core.ForgeAbstractions;
import focuss.fluidcombat.particles.ModParticlesForge;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(FluidCombat.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class FluidCombatForge {

    @SubscribeEvent
    public static void onConstructMod(final FMLConstructModEvent evt) {
        ModConstructor.construct(FluidCombat.MOD_ID, FluidCombat::new);

        ModParticlesForge.PARTICLE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());

        // only on the client side…
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // register our FMLClientSetup listener
            FMLJavaModLoadingContext.get()
                .getModEventBus()
                .addListener(FluidCombatForgeClient::onClientSetup);
        }

        System.out.println("FluidCombat (Forge): onConstructMod ran");
    }
}