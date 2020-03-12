package com.function.main.controlstuff;

public class ReturnValue extends RuntimeException {

    private Value value;

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }
}
