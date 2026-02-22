package me.cipher.existence.util;

import java.util.UUID;

public interface OwnerAwareItemEntity {
    UUID getOwnerUUID();
    void setOwnerUUID(UUID uuid);
}