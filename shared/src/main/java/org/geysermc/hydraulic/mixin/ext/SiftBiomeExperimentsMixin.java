package org.geysermc.hydraulic.mixin.ext;

import org.cloudburstmc.protocol.bedrock.data.ExperimentData;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.compat.SiftBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom biome IDs require the client experiment as well as their definitions. */
@Mixin(value = GeyserSession.class, remap = false)
public class SiftBiomeExperimentsMixin {
    @Inject(method = "configureExperiments", at = @At("TAIL"))
    private void hydraulic$enableCustomBiomes(StartGamePacket packet, CallbackInfo ci) {
        if (!SiftBiomes.IDS.isEmpty()) packet.getExperiments().add(new ExperimentData("data_driven_biomes", true));
        ((org.geysermc.hydraulic.compat.SessionTrafficAccess)this).hydraulic$traffic().loginSettings(
                "experiments="+packet.getExperiments()+" customBiomeCount="+SiftBiomes.IDS.size());
    }
}
