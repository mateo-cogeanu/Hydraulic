package org.geysermc.hydraulic.pack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Bedrock caches packs by UUID/version, so converter updates must change pack identity too. */
public final class PackIdentity {
    public static UUID of(UUID modContent, UUID converterContent) {
        return UUID.nameUUIDFromBytes(("hydraulic-pack-v2:" + modContent + ":" + converterContent).getBytes(StandardCharsets.UTF_8));
    }
}
