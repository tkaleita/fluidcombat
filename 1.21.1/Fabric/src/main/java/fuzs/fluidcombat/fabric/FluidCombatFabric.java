package fuzs.fluidcombat.fabric;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.helper.DebugTransform;
import fuzs.puzzleslib.api.client.event.v1.ClientTickEvents;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric‑only client bootstrap that registers debug keybinds for tweaking the
 * guard‑pose transform live in‑game.
 */
public final class FluidCombatFabric implements ModInitializer {

    /* ───────────────────────────────
     *  Key bindings – all six axes
     * ─────────────────────────────── */
    // TRANSLATE
    private static final KeyMapping T_X_POS = key("tx_pos", GLFW.GLFW_KEY_KP_6);
    private static final KeyMapping T_X_NEG = key("tx_neg", GLFW.GLFW_KEY_KP_4);

    private static final KeyMapping T_Y_POS = key("ty_pos", GLFW.GLFW_KEY_KP_ADD);      // Numpad +
    private static final KeyMapping T_Y_NEG = key("ty_neg", GLFW.GLFW_KEY_KP_SUBTRACT); // Numpad −

    private static final KeyMapping T_Z_POS = key("tz_pos", GLFW.GLFW_KEY_KP_8);
    private static final KeyMapping T_Z_NEG = key("tz_neg", GLFW.GLFW_KEY_KP_2);

    // ROTATE
    private static final KeyMapping R_X_POS = key("rx_pos", GLFW.GLFW_KEY_KP_7);
    private static final KeyMapping R_X_NEG = key("rx_neg", GLFW.GLFW_KEY_KP_1);

    private static final KeyMapping R_Y_POS = key("ry_pos", GLFW.GLFW_KEY_KP_9);
    private static final KeyMapping R_Y_NEG = key("ry_neg", GLFW.GLFW_KEY_KP_3);

    private static final KeyMapping R_Z_POS = key("rz_pos", GLFW.GLFW_KEY_KP_DIVIDE);   // Numpad /
    private static final KeyMapping R_Z_NEG = key("rz_neg", GLFW.GLFW_KEY_KP_MULTIPLY); // Numpad *

    // LOG CURRENT VALUES
    private static final KeyMapping LOG      = key("log_values", GLFW.GLFW_KEY_KP_5);

    /* Helper to create a KeyMapping with the debug category */
    private static KeyMapping key(String id, int keyCode) {
        return new KeyMapping("key.fluidcombat." + id, keyCode, "key.categories.fluidcombat.debug");
    }

    /* ───────────────────────────────
     *  Registration
     * ─────────────────────────────── */
    @Override
    public void onInitialize() {
        ModConstructor.construct(FluidCombat.MOD_ID, FluidCombat::new);

        // DEBUG BINDINGS
        /*register(T_X_POS, T_X_NEG, T_Y_POS, T_Y_NEG, T_Z_POS, T_Z_NEG,
                 R_X_POS, R_X_NEG, R_Y_POS, R_Y_NEG, R_Z_POS, R_Z_NEG,
                 LOG);

        // Each client tick: adjust transform if keys held, print if log pressed 
        ClientTickEvents.END.register(client -> {
            // Translation (0.01 blocks per tick)
            if (T_X_POS.isDown()) DebugTransform.translateX += 0.01F;
            if (T_X_NEG.isDown()) DebugTransform.translateX -= 0.01F;
            if (T_Y_POS.isDown()) DebugTransform.translateY += 0.01F;
            if (T_Y_NEG.isDown()) DebugTransform.translateY -= 0.01F;
            if (T_Z_POS.isDown()) DebugTransform.translateZ += 0.01F;
            if (T_Z_NEG.isDown()) DebugTransform.translateZ -= 0.01F;

            // Rotation (1° per tick)
            if (R_X_POS.isDown()) DebugTransform.rotateX += 1F;
            if (R_X_NEG.isDown()) DebugTransform.rotateX -= 1F;
            if (R_Y_POS.isDown()) DebugTransform.rotateY += 1F;
            if (R_Y_NEG.isDown()) DebugTransform.rotateY -= 1F;
            if (R_Z_POS.isDown()) DebugTransform.rotateZ += 1F;
            if (R_Z_NEG.isDown()) DebugTransform.rotateZ -= 1F;

            // Print current values
            if (LOG.consumeClick()) {
                DebugTransform.logValues();
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal(DebugTransform.currentAsString()));
                }
            }
        });*/
    }

    /* Utility: bulk‑register */
    private static void register(KeyMapping... mappings) {
        for (KeyMapping m : mappings) KeyBindingHelper.registerKeyBinding(m);
    }
}
