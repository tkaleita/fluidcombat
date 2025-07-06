package fuzs.fluidcombat.helper;

import fuzs.fluidcombat.FluidCombat;

public final class DebugTransform {
    public static float translateX, translateY, translateZ;
    public static float rotateX, rotateY, rotateZ;

    public static void logValues() {
        FluidCombat.LOGGER.info(currentAsString());
    }

    public static String currentAsString() {
        return String.format(
            "translate(%.3f, %.3f, %.3f)  rotate(%.1f°, %.1f°, %.1f°)",
            translateX, translateY, translateZ,
            rotateX, rotateY, rotateZ
        );
    }
}
