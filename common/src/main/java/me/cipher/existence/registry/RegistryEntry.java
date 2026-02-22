package me.cipher.existence.registry;

import java.util.function.Supplier;

public class RegistryEntry<T> implements Supplier<T> {
    private final String id;
    private final Supplier<T> supplier;
    private T value;

    public RegistryEntry(String id, Supplier<T> supplier) {
        this.id = id;
        this.supplier = supplier;
    }

    public String getId() {
        return id;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new IllegalStateException("Registry entry " + id + " not initialized yet!");
        }
        return value;
    }

    public void initialize(T value) {
        this.value = value;
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }
}