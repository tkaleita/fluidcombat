package focuss.fluidcombat.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;

public class ClientConfig implements ConfigCore {
    @Config(description = "Show a shield indicator similar to the attack indicator when actively blocking.")
    public boolean shieldIndicator = true;
    @Config(description = "Show particles on attack that show range and sweep radius.")
    public boolean showSweepTubeParticles = true;
    @Config(description = "Show sweep attack particle on each attack.")
    public boolean showSweepAttackParticles = true;
    @Config(description = "Use alternative swing animation.")
    public boolean alternativeSwingAnimation = true;
    @Config(description = "If player holds the same type of item in both hands, automatically alternate attacks.")
    public boolean alternatingAttacks = true;
    @Config(description = "If alternating attacks are enabled, this skips the check if the item types are the same. Effectively allowing you to alternate attacks between any items.")
    public boolean unrestrictedAlternatingAttacks = true;
}
