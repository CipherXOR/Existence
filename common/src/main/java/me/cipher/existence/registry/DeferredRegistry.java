package me.cipher.existence.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DeferredRegistry<T> {
    private final String modId;
    private final String registryName;
    private final List<RegistryEntry<? extends T>> entries = new ArrayList<>();

    public DeferredRegistry(String modId, String registryName) {
        this.modId = modId;
        this.registryName = registryName;
    }

    public <R extends T> RegistryEntry<R> register(String id, Supplier<R> supplier) {
        RegistryEntry<R> entry = new RegistryEntry<>(modId + ":" + id, supplier);
        entries.add(entry);
        return entry;
    }

    public List<RegistryEntry<? extends T>> getEntries() {
        return entries;
    }

    public String getRegistryName() {
        return registryName;
    }
}