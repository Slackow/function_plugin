package com.function.main.controlstuff;

import java.util.HashMap;

public class Scope<T> extends HashMap<String, T> {
    public Scope<T> getParent() {
        return defaults;
    }

    private Scope<T> defaults;

    public Scope(Scope<T> defaults) {
        this.defaults = defaults;
    }

    public Scope() {
        this(null);
    }

    public T getProperty(String key) {
        T value = super.get(key);
        return value == null && defaults != null ? defaults.getProperty(key) : value;
    }

    public void setProperty(String key, T value) {
        Scope<T> scope = this;
        while (scope != null && !scope.containsKey(key)) {
            scope = scope.defaults;
        }
        (scope == null ? this : scope).put(key, value);
    }
}
