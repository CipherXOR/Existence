package me.cipher.existence.util;

import java.util.UUID;

public interface VisibilityChecker {
    boolean isVisible(UUID viewer, UUID target);
}