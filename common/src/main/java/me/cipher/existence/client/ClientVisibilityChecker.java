package me.cipher.existence.client;

import me.cipher.existence.util.VisibilityChecker;

import java.util.UUID;

public class ClientVisibilityChecker implements VisibilityChecker {
    @Override
    public boolean isVisible(UUID viewer, UUID target) {
        if (viewer.equals(target)) return true;
        return VisiblePlayerManager.isVisible(target);
    }
}