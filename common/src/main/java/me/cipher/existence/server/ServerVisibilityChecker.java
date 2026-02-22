package me.cipher.existence.server;

import me.cipher.existence.util.VisibilityChecker;

import java.util.UUID;

public class ServerVisibilityChecker implements VisibilityChecker {
    @Override
    public boolean isVisible(UUID viewer, UUID target) {
        if (viewer.equals(target)) return true;
        return ServerVisibleManager.isVisible(viewer, target);
    }
}