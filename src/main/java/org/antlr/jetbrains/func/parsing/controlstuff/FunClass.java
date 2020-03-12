package com.function.main.controlstuff;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import static com.function.main.grammar.MainFunctionParser.ExprContext;

public class FunClass extends Value {
    public FunClass() {
        super(new Object());
    }

    private Map<Integer, FunFunction> constructors = new HashMap<>();
    private Map<String, ExprContext> instanceVars = new HashMap<>();
    private Map<String, Function<Value, FunFunction>> instanceFunctions = new HashMap<>();

    @Override
    public boolean isClass() {
        return true;
    }

    public Map<String, ExprContext> getInstanceVars() {
        return instanceVars;
    }

    public Map<Integer, FunFunction> getConstructors() {
        return constructors;
    }

    public Map<String, Function<Value, FunFunction>> getInstanceFunctions() {
        return instanceFunctions;
    }

    @Override
    public FunClass asClass(Supplier<? extends RuntimeException> ex) {
        return this;
    }
}
