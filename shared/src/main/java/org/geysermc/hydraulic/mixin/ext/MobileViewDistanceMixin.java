package org.geysermc.hydraulic.mixin.ext;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.compat.MobileViewDistance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GeyserSession.class, remap = false)
public class MobileViewDistanceMixin {
    @ModifyReturnValue(method = "getRenderDistance", at = @At("RETURN"))
    private int hydraulic$limitMobileServerView(int requested) {
        var client = ((GeyserSession) (Object) this).getClientData();
        return client == null ? requested : MobileViewDistance.limit(requested, client.getDeviceOs().name());
    }
}
