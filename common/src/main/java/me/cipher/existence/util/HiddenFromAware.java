package me.cipher.existence.util;

import java.util.UUID;

public interface HiddenFromAware {
    UUID getHiddenFrom();
    void setHiddenFrom(UUID uuid);
}