package com.function.main.controlstuff;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunFunction extends Value {

    public Function<List<Value>, Value> getFunction() {
        return function;
    }

    @Override
    public FunFunction asFunction(Supplier<? extends RuntimeException> ex) {
        return this;
    }

    public Value getInstance() {
        return instance;
    }

    private List<String> params;
    private Function<List<Value>, Value> function;
    private Value instance;

    public FunFunction(List<String> params, Function<List<Value>, Value> function) {
        this(params, function, null);
    }

    public FunFunction(List<String> params, Function<List<Value>, Value> function, Value instance) {
        super(new Object());
        this.params = params;
        this.function = function;
        this.instance = instance;
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    public List<String> getParams() {
        return params;
    }
}
