package com.function.main.controlstuff;

import com.function.main.CompileStuff;
import com.function.main.Datapack;
import com.function.main.grammar.MainFunctionParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.function.main.controlstuff.Value.VOID;
import static com.function.main.grammar.MainFunctionParser.*;

public class FunctionVisitor extends MainFunctionParserBaseVisitor<Value> {
    private Scope<Value> memory = new Scope<>();
    private Datapack datapack;
    private Stack<String> namespaceStack = new Stack<>();

    public FunctionVisitor(Datapack datapack) {
        this.datapack = datapack;
    }

    private void enterScope() {
        memory = new Scope<>(memory);
    }

    private void exitScope() {
        memory = memory.getParent();
    }

    public void addNamespace(String namespace) {
        namespaceStack.add(namespace);
    }

    @Override
    public Value visitProgram(ProgramContext ctx) {
        if (ctx.THIS() != null) {
            if (ctx.TICK() != null) {
                datapack.addTicked(namespaceStack.get(0));
            } else {
                datapack.addLoaded(namespaceStack.get(0));
            }
        }
        ctx.statement().forEach(this::visit);
        return VOID;
    }

    @Override
    public Value visitFunction_class(Function_classContext ctx) {
        String className = ctx.IDEN().getText();
        FunClass funClass = new FunClass();
        for (Class_lineContext class_lineContext : ctx.class_line()) {
            if (class_lineContext instanceof ConstantLineContext) {
                ConstantLineContext context = (ConstantLineContext) class_lineContext;
                Var_definitionContext var_definitionContext = context.var_definition();
                if (context.INSTANCE() != null) {
                    funClass.getInstanceVars().put(var_definitionContext.IDEN(0).getText(), var_definitionContext.expr());
                } else {
                    funClass.getProperties().put(var_definitionContext.IDEN(0).getText(), this.visit(var_definitionContext.expr()));
                }
            } else if (class_lineContext instanceof FunctionClassLineContext) {
                FunctionClassLineContext context = (FunctionClassLineContext) class_lineContext;
                String methodName = context.IDEN(0).getText();
                List<String> idens = context.IDEN().stream().map(ParseTree::getText).skip(1).collect(Collectors.toList());

                if (context.INSTANCE() != null) {
                    funClass.getInstanceFunctions().put(
                            methodName,
                            value -> new FunFunction(idens, input -> {
                                try {
                                    this.visit(context.block());
                                } catch (ReturnValue returnValue) {
                                    return returnValue.getValue();
                                }
                                return VOID;
                            }, value)
                    );
                } else {
                    FunFunction function = new FunFunction(idens, a -> {
                        try {
                            this.visit(context.block());
                        } catch (ReturnValue returnValue) {
                            // using Exceptions a little badly but whatever
                            return returnValue.getValue();
                        }
                        return VOID;
                    }, null);
                    funClass.getProperties().put(methodName, function);
                }

            } else if (class_lineContext instanceof ConstructorClassLineContext) {
                ConstructorClassLineContext context = (ConstructorClassLineContext) class_lineContext;
                List<String> idens = context.IDEN().stream().map(ParseTree::getText).collect(Collectors.toList());
                funClass.getConstructors().putIfAbsent(idens.size(), new FunFunction(idens, values -> {
                    Value value = Value.valueOf(Collections.emptyMap());
                    enterScope();
                    memory.put("this", value);
                    Map<String, Value> properties = value.getProperties();
                    funClass.getInstanceFunctions().forEach((key, val) -> properties.put(key, val.apply(value)));
                    funClass.getInstanceVars().forEach((key, val) -> properties.put(key, this.visit(val)));
                    for (int i = 0; i < idens.size(); i++) {
                        memory.put(idens.get(i), values.get(i));
                    }
                    this.visit(context.block());
                    exitScope();
                    return value;
                }));
            }
        }
        memory.put(className, funClass);
        return VOID;
    }


    @Override
    public Value visitVar_definition(Var_definitionContext ctx) {
        Value value = this.visit(ctx.expr());
        int size = ctx.IDEN().size();
        if (size == 1) {
            memory.put(ctx.IDEN(0).getText(), value);
        } else {
            List<String> keys = ctx.IDEN().stream().map(ParseTree::getText).collect(Collectors.toList());
            List<Value> list = value.asList(supplier(ctx));

            if (list.size() == size) {
                for (int i = 0; i < keys.size(); i++) {
                    memory.put(keys.get(i), list.get(i));
                }
            } else {
                throw new EvalException("list different length than expected: " + list.size(), ctx);
            }

        }
        return VOID;
    }

    @Override
    public Value visitVarDeclarationMod(VarDeclarationModContext ctx) {
        String id = ctx.IDEN().getText();
        Value value = this.visit(ctx.expr());
        Supplier<EvalException> supplier = supplier(ctx);

        Value oldValue = memory.getProperty(id);
        if (oldValue == null) {
            throw new EvalException("no such var: " + id, ctx);
        }
        switch (ctx.var_operator.getType()) {
            case PLUSEQUAL:
                value = oldValue.isDouble() && value.isDouble() ?
                        Value.valueOf(oldValue.asDouble(supplier) + value.asDouble(supplier)) :
                        Value.valueOf(oldValue.asString() + value.asString());
                break;
            case MINUSEQUAL:
                value = Value.valueOf(oldValue.asDouble(supplier) - value.asDouble(supplier));
                break;
            case MULTEQUAL:
                value = Value.valueOf(oldValue.asDouble(supplier) * value.asDouble(supplier));
                break;
            case DIVIDEEQUAL:
                value = Value.valueOf(oldValue.asDouble(supplier) / value.asDouble(supplier));
                break;
        }
        memory.setProperty(id, value);
        return VOID;
    }

    private static Supplier<EvalException> supplier(ParserRuleContext ctx) {
        return () -> new EvalException(ctx);
    }

    @Override
    public Value visitVarDeclarationModPlusPlus(VarDeclarationModPlusPlusContext ctx) {
        String id = ctx.IDEN().getText();
        Value value = memory.getProperty(id);
        switch (ctx.var_operator.getType()) {
            case PLUSPLUS:
                value = Value.valueOf(value.asDouble(supplier(ctx)) + 1);
                break;
            case MINUSMINUS:
                value = Value.valueOf(value.asDouble(supplier(ctx)) - 1);
                break;
        }
        memory.setProperty(id, value);
        return value;
    }

    @Override
    public Value visitVarDeclarationListItem(VarDeclarationListItemContext ctx) {
        Value list = this.visit(ctx.expr(0));
        Value index = this.visit(ctx.expr(1));
        Value value = this.visit(ctx.expr(2));
        Supplier<EvalException> supplier = supplier(ctx);
        int type = ctx.var_operator.getType();
        if (type != EQUAL) {
            Value oldValue;
            if (index.isInteger()) {
                oldValue = list.asList(supplier).get(index.asInteger(supplier));
            } else {
                if (!list.isObject() && !list.isClass()) {
                    throw new EvalException("not an object: " + list, ctx);
                }
                oldValue = list.getProperties().get(index.asString());
            }
            switch (type) {
                case PLUSEQUAL:
                    value = oldValue.isDouble() && value.isDouble() ?
                            Value.valueOf(oldValue.asDouble(supplier) + value.asDouble(supplier)) :
                            Value.valueOf(oldValue.asString() + value.asString());
                    break;
                case MINUSEQUAL:
                    value = Value.valueOf(oldValue.asDouble(supplier) - value.asDouble(supplier));
                    break;
                case MULTEQUAL:
                    value = Value.valueOf(oldValue.asDouble(supplier) * value.asDouble(supplier));
                    break;
                case DIVIDEEQUAL:
                    value = Value.valueOf(oldValue.asDouble(supplier) / value.asDouble(supplier));
                    break;
            }
        }

        if (index.isInteger()) {
            list.asList(supplier).set(index.asInteger(supplier), value);
        } else {
            list.getProperties().put(index.asString(), value);
        }
        return value;
    }

    @Override
    public Value visitVarDeclarationListItemPlusPlus(VarDeclarationListItemPlusPlusContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        Value list = this.visit(ctx.expr(0));
        Value index = this.visit(ctx.expr(1));
        Value value = list.isList() ?
                list.asList(supplier).get(index.asInteger(supplier)) :
                list.getProperties().get(index.asString());
        switch (ctx.var_operator.getType()) {
            case PLUSPLUS:
                value = Value.valueOf(value.asDouble(supplier) + 1);
                break;
            case MINUSMINUS:
                value = Value.valueOf(value.asDouble(supplier) - 1);
                break;
        }
        if (index.isInteger()) {
            list.asList(supplier).set(index.asInteger(supplier), value);
        } else {
            list.getProperties().put(index.asString(), value);
        }
        return value;
    }

    @Override
    public Value visitVarDeclarationObjectItem(VarDeclarationObjectItemContext ctx) {
        String key = ctx.IDEN().getText();
        Value object = this.visit(ctx.expr(0));
        Value value = this.visit(ctx.expr(1));

        Supplier<EvalException> supplier = supplier(ctx);

        if (!object.isObject() && !object.isClass()) {
            throw new EvalException("not an object: " + object, ctx);
        }

        int type = ctx.var_operator.getType();

        if (type != EQUAL) {
            Value oldValue = object.getProperties().get(key);
            switch (type) {
                case PLUSEQUAL:
                    value = oldValue.isDouble() && value.isDouble() ?
                            Value.valueOf(oldValue.asDouble(supplier) + value.asDouble(supplier)) :
                            Value.valueOf(oldValue.asString() + value.asString());
                    break;
                case MINUSEQUAL:
                    value = Value.valueOf(oldValue.asDouble(supplier) - value.asDouble(supplier));
                    break;
                case MULTEQUAL:
                    value = Value.valueOf(oldValue.asDouble(supplier) * value.asDouble(supplier));
                    break;
                case DIVIDEEQUAL:
                    value = Value.valueOf(oldValue.asDouble(supplier) / value.asDouble(supplier));
                    break;
            }
        }
        object.getProperties().put(key, value);
        return value;
    }

    @Override
    public Value visitVarDeclarationObjectItemPlusPlus(VarDeclarationObjectItemPlusPlusContext ctx) {
        String key = ctx.IDEN().getText();
        Value object = this.visit(ctx.expr());
        Value oldValue = object.getProperties().get(key);
        Value value;
        Supplier<EvalException> supplier = supplier(ctx);
        if (!object.isObject() && !object.isClass()) {
            throw new EvalException("not an object: " + object, ctx);
        }

        switch (ctx.var_operator.getType()) {
            case PLUSPLUS:
                value = Value.valueOf(oldValue.asDouble(supplier) + 1);
                break;
            case MINUSMINUS:
                value = Value.valueOf(oldValue.asDouble(supplier) - 1);
                break;
            default:
                throw new EvalException(ctx);
        }
        object.getProperties().put(key, value);
        return value;
    }

    @Override
    public Value visitIdentifierAtom(IdentifierAtomContext ctx) {
        String id = ctx.getText();
        Value property = memory.getProperty(id);
        if (property == null) {
            throw new EvalException("no such var: " + id, ctx);
        }
        return property;
    }

    @Override
    public Value visitStringAtom(StringAtomContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (String_partContext context : ctx.string_part()) {
            if (context instanceof IdInterpPartContext) {                      // remove dollar sign
                String id = ((IdInterpPartContext) context).ID_INTERP().getText().substring(1);
                sb.append(memory.getProperty(id).asString());
            } else if (context instanceof ExprInterpPartContext) {
                Value value = this.visit(((ExprInterpPartContext) context).expr());
                sb.append(value);
            } else if (context instanceof EscapeStringPartContext) {
                String text = context.getText().substring(1);
                if (text.startsWith("u")) {
                    sb.append(Character.toChars(Integer.parseInt(text.substring(1), 16)));
                } else if ("r".equals(text)) {
                    sb.append('\r');
                } else if ("n".equals(text)) {
                    sb.append('\n');
                } else { // \$ | \' | \" | \\
                    sb.append(text);
                }
            } else {
                sb.append(context.getText());
            }
        }
        return Value.valueOf(sb.toString());
    }


    @Override
    public Value visitListAtom(ListAtomContext ctx) {
        return Value.valueOf(ctx.expr().stream().map(this::visit).collect(Collectors.toList()));
    }

    @Override
    public Value visitObjectAtom(ObjectAtomContext ctx) {
        List<TerminalNode> keys = ctx.IDEN();
        List<ExprContext> values = ctx.expr();
        return Value.valueOf(
                IntStream.range(0, keys.size())
                        .collect(
                                HashMap::new,
                                (Map<String, Value> m, int i) ->
                                        m.put(keys.get(i).getText(), this.visit(values.get(i))),
                                Map::putAll
                        )
        );
    }

    @Override
    public Value visitNumAtom(NumAtomContext ctx) {
        return Value.valueOf(Double.valueOf(ctx.getText().replace("_", "")));
    }

    @Override
    public Value visitBooleanAtom(BooleanAtomContext ctx) {
        return Value.valueOf(Boolean.valueOf(ctx.getText()));
    }

    @Override
    public Value visitAtomExpr(AtomExprContext ctx) {
        return this.visit(ctx.atom());
    }

    @Override
    public Value visitParExpr(ParExprContext ctx) {
        return this.visit(ctx.expr());
    }

    @Override
    public Value visitPowExpr(PowExprContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        return Value.valueOf(Math.pow(this.visit(ctx.expr(0)).asDouble(supplier), this.visit(ctx.expr(1)).asDouble(supplier)));
    }

    @Override
    public Value visitNegationExpr(NegationExprContext ctx) {
        return Value.valueOf(-this.visit(ctx.expr()).asDouble(supplier(ctx)));
    }

    @Override
    public Value visitNotExpr(NotExprContext ctx) {
        return Value.valueOf(!this.visit(ctx.expr()).asBoolean(supplier(ctx)));
    }

    @Override
    public Value visitMultiplicationExpr(MultiplicationExprContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        Double left = this.visit(ctx.expr(0)).asDouble(supplier);
        Double right = this.visit(ctx.expr(1)).asDouble(supplier);
        switch (ctx.op.getType()) {
            case MULT:
                return Value.valueOf(left * right);
            case DIV:
                return Value.valueOf(left / right);
            case MOD:
                return Value.valueOf(left % right);
            default:
                throw new EvalException("unknown operator: " + VOCABULARY.getLiteralName(ctx.op.getType()), ctx);
        }
    }

    @Override
    public Value visitAdditiveExpr(AdditiveExprContext ctx) {
        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));
        Supplier<EvalException> supplier = supplier(ctx);
        switch (ctx.op.getType()) {
            case PLUS:
                return left.isDouble() && right.isDouble() ?
                        Value.valueOf(left.asDouble(supplier) + right.asDouble(supplier)) :
                        Value.valueOf(left.asString() + right.asString());
            case MINUS:
                return Value.valueOf(left.asDouble(supplier) - right.asDouble(supplier));
            default:
                throw new EvalException("unknown operator: " + VOCABULARY.getLiteralName(ctx.op.getType()), ctx);
        }
    }

    @Override
    public Value visitRelationalExpr(RelationalExprContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        Double left = this.visit(ctx.expr(0)).asDouble(supplier);
        Double right = this.visit(ctx.expr(1)).asDouble(supplier);
        switch (ctx.op.getType()) {
            case LT:
                return Value.valueOf(left < right);
            case LE:
                return Value.valueOf(left <= right);
            case GT:
                return Value.valueOf(left > right);
            case GE:
                return Value.valueOf(left >= right);
            default:
                throw new EvalException("unknown operator: " + VOCABULARY.getLiteralName(ctx.op.getType()), ctx);
        }
    }

    @Override
    public Value visitEqualityExpr(EqualityExprContext ctx) {
        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));
        switch (ctx.op.getType()) {
            case EQ:
                return Value.valueOf(Objects.equals(left, right));
            case NE:
                return Value.valueOf(!Objects.equals(left, right));
            default:
                throw new EvalException("unknown operator: " + VOCABULARY.getLiteralName(ctx.op.getType()), ctx);
        }
    }

    @Override
    public Value visitAndExpr(AndExprContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        return Value.valueOf(this.visit(ctx.expr(0)).asBoolean(supplier) && this.visit(ctx.expr(1)).asBoolean(supplier));
    }

    @Override
    public Value visitOrExpr(OrExprContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        return Value.valueOf(this.visit(ctx.expr(0)).asBoolean(supplier) || this.visit(ctx.expr(1)).asBoolean(supplier));
    }

    @Override
    public Value visitGetItemExpr(GetItemExprContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        Value source = this.visit(ctx.expr(0));
        Value location = this.visit(ctx.expr(1));
        if (source.isList()) {
            return source.asList(null).get(location.asInteger(supplier));
        } else if (source.isString()) {
            return Value.valueOf(source.asString().substring(location.asInteger(supplier), 1));
        } else {
            return source.getProperties().get(location.asString());
        }
    }

    @Override
    public Value visitSubExpr(SubExprContext ctx) {
        int begin;
        int end;
        int inc;
        Supplier<EvalException> supplier = supplier(ctx);

        begin = this.visit(ctx.expr(1)).asInteger(supplier);
        end = this.visit(ctx.expr(2)).asInteger(supplier);
        if (ctx.expr().size() == 3) {
            inc = begin < end ? 1 : -1;
        } else {
            inc = this.visit(ctx.expr(3)).asInteger(supplier);
        }

        Value value = this.visit(ctx.expr(0));
        if (value.isList()) {
            List<Value> list = new ArrayList<>();
            List<Value> values = value.asList(supplier);
            if (inc > 0) {
                for (int i = begin; i <= end; i += inc) {
                    list.add(values.get(i));
                }
            } else {
                for (int i = begin; i >= end; i += inc) {
                    list.add(values.get(i));
                }
            }
            return Value.valueOf(list);
        } else {
            StringBuilder sb = new StringBuilder();
            String valueString = value.asString();

            if (inc == 1) {
                return Value.valueOf(valueString.substring(begin, end));
            }

            for (int i = begin; i < end; i += inc) {
                sb.append(valueString.charAt(i));
            }
            return Value.valueOf(sb.toString());
        }
    }

    @Override
    public Value visitAssert_line(Assert_lineContext ctx) {
        if (this.visit(ctx.expr()).asBoolean(supplier(ctx))) {
            return VOID;
        }
        throw new AssertionError(ctx.expr().getText() + " returned false");
    }

    @Override
    public Value visitLog_line(Log_lineContext ctx) {
        Value value = this.visit(ctx.expr());
        CompileStuff.LOGGER.info(value.asString());
        return VOID;
    }

    @Override
    public Value visitIf_statement(If_statementContext ctx) {
        Expr_blockContext blockContext = ctx.expr_block();
        if (this.visit(blockContext.expr()).asBoolean(supplier(ctx))) {
            enterScope();
            this.visit(blockContext.stat_block());
            exitScope();
        }
        if (ctx.stat_block() != null) {
            enterScope();
            this.visit(ctx.stat_block());
            exitScope();
        }
        return VOID;
    }

    @Override
    public Value visitNullAtom(NullAtomContext ctx) {
        return Value.NULL;
    }

    @Override
    public Value visitEnding_line(Ending_lineContext ctx) {
        if (ctx.RETURN() != null) {
            return ctx.expr() == null ? VOID : this.visit(ctx.expr());
        } else if (ctx.BREAK() != null) {
            return Value.BREAK;
        } else if (ctx.CONTINUE() != null) {
            return Value.CONTINUE;
        } else {
            throw new EvalException("Unknown EndingLine: ", ctx);
        }
    }

    @Override
    public Value visitRunFunctionExpr(RunFunctionExprContext ctx) {
        Value source = this.visit(ctx.expr(0));
        List<Value> params = ctx.expr().stream().skip(1).map(this::visit).collect(Collectors.toList());
        if (source.isFunction()) {
            FunFunction fun = source.asFunction(supplier(ctx));
            enterScope();
            memory.put("this", fun.getInstance());
            Value value = fun.getFunction().apply(params);
            exitScope();
            return value;
        }
        if (source.isClass()) {
            FunClass funClass = source.asClass(supplier(ctx));
            FunFunction constructor = funClass.getConstructors().get(params.size());
            enterScope();
            Value value = constructor.getFunction().apply(params);
            exitScope();
            return value;
        }
        return VOID;
    }

    @Override
    public Value visitFunction_call_line(Function_call_lineContext ctx) {
        Value function = this.visit(ctx.expr(0));
        List<Value> params = ctx.expr().stream().skip(1).map(this::visit).collect(Collectors.toList());
        FunFunction fun = function.asFunction(supplier(ctx));
        enterScope();
        memory.put("this", fun.getInstance());
        Value value = fun.getFunction().apply(params);
        exitScope();
        return value;

    }

    // (IDEN | LPAREN (IDEN (COMMA IDEN)*)? RPAREN) ARROW (expr | block)

    @Override
    public Value visitLambdaAtom(LambdaAtomContext ctx) {
        List<String> idens = ctx.IDEN().stream().map(ParseTree::getText).collect(Collectors.toList());
        return new FunFunction(idens, values -> {
            if (values.size() != idens.size()) {
                throw new EvalException("Invalid parameter input stuff expected: " + idens + " got " + values, ctx);
            }
            for (int i = 0; i < idens.size(); i++) {
                String iden = idens.get(i);
                memory.put(iden, values.get(i));
            }

            if (ctx.expr() != null) {
                return this.visit(ctx.expr());
            }

            try {
                this.visit(ctx.block());
            } catch (ReturnValue returnValue) {
                return returnValue.getValue();
            }
            return VOID;
        });
    }

    @Override
    public Value visitFunction_line(Function_lineContext ctx) {
        String name = ctx.IDEN(0).getText();
        List<String> idens = ctx.IDEN().stream().skip(1).map(ParseTree::getText).collect(Collectors.toList());
        FunFunction funFunction = new FunFunction(idens, values -> {
            if (values.size() != idens.size()) {
                throw new EvalException("Invalid parameter input stuff expected: " + idens + " got " + values, ctx);
            }
            for (int i = 0; i < idens.size(); i++) {
                String iden = idens.get(i);
                memory.put(iden, values.get(i));
            }

            try {
                this.visit(ctx.block());
            } catch (ReturnValue returnValue) {
                return returnValue.getValue();
            }
            return VOID;
        });
        memory.put(name, funFunction);
        return VOID;
    }

    private static ReturnValue returnValue = new ReturnValue();

    @Override
    public Value visitStat_block(Stat_blockContext ctx) {
        StatementContext statement = ctx.statement();
        if (statement != null) {
            this.visit(statement);
            return VOID;
        }
        Ending_lineContext endingLine = ctx.ending_line();
        if (endingLine != null) {
            returnValue.setValue(this.visit(endingLine));
            throw returnValue;
        }

        this.visit(ctx.block());


        return VOID;
    }

    @Override
    public Value visitBlock(BlockContext ctx) {
        for (StatementContext statementContext : ctx.statement()) {
            this.visit(statementContext);
        }

        if ((ctx.ending_line()) != null) {
            returnValue.setValue(this.visit(ctx.ending_line()));
            throw returnValue;
        }
        return VOID;
    }

    @Override
    public Value visitWhile_loop(While_loopContext ctx) {
        while (this.visit(ctx.expr_block().expr()).asBoolean(supplier(ctx))) {
            try {
                enterScope();
                this.visit(ctx.expr_block().stat_block());
                exitScope();
            } catch (ReturnValue returnValue) {
                Value value = returnValue.getValue();
                exitScope();
                if (value == Value.BREAK) {
                    break;
                } else if (value != Value.CONTINUE) {
                    throw returnValue;
                }
            }
        }

        return VOID;
    }

    @Override
    public Value visitFor_loop(For_loopContext ctx) {
        ExprContext expr = ctx.expr();
        LineContext line1 = ctx.line(0);
        LineContext line2 = ctx.line(1);
        Supplier<EvalException> supplier = supplier(ctx);
        enterScope();
        if (line1 != null) {
            this.visit(line1);
        }
        while (expr == null || this.visit(expr).asBoolean(supplier)) {

            try {
                this.visit(ctx.stat_block());
            } catch (ReturnValue returnValue) {
                Value value = returnValue.getValue();
                if (value == Value.BREAK) {
                    break;
                } else if (value != Value.CONTINUE) {
                    exitScope();
                    throw returnValue;
                }
            }
            if (line2 != null) {
                this.visit(line2);
            }
        }
        exitScope();
        return VOID;
    }

    @Override
    public Value visitDo_while_loop(Do_while_loopContext ctx) {
        Supplier<EvalException> supplier = supplier(ctx);
        do {
            try {
                enterScope();
                this.visit(ctx.stat_block());
                exitScope();
            } catch (ReturnValue returnValue) {
                Value value = returnValue.getValue();
                if (value == Value.BREAK) {
                    break;
                } else if (value != Value.CONTINUE) {
                    throw returnValue;
                }
            }
        } while (this.visit(ctx.expr()).asBoolean(supplier));
        return VOID;
    }

    @Override
    public Value visitForeach_loop(Foreach_loopContext ctx) {
        int index = 0;
        List<TerminalNode> iden = ctx.IDEN();
        String id;
        String indexID;
        if (iden.size() == 2) {
            id = iden.get(1).getText();
            indexID = iden.get(0).getText();
        } else {
            id = iden.get(0).getText();
            indexID = "i";
        }
        Expr_blockContext expr_blockContext = ctx.expr_block();
        for (Value val : this.visit(expr_blockContext.expr()).asList(supplier(ctx))) {
            enterScope();
            memory.put(indexID, Value.valueOf(index++));
            memory.put(id, val);
            try {
                this.visit(expr_blockContext.stat_block());
            } catch (ReturnValue returnValue) {
                Value value = returnValue.getValue();
                if (value == Value.BREAK) {
                    exitScope();
                    break;
                } else if (value != Value.CONTINUE) {
                    exitScope();
                    throw returnValue;
                }
            }
            exitScope();
        }
        return VOID;
    }

    @Override
    public Value visitGetFromObjectExpr(GetFromObjectExprContext ctx) {
        Value object = this.visit(ctx.expr());
        String child = ctx.IDEN().getText();

        Map<String, Value> properties = object.getProperties();
        if (ctx.QUESTION() != null && (object == Value.NULL || !properties.containsKey(child))) {
            return Value.NULL;
        }

        return properties.get(child);
    }


    @Override
    public Value visitThisAtom(ThisAtomContext ctx) {
        return memory.getProperty("this");
    }


    @Override
    public Value visitTernaryExpr(TernaryExprContext ctx) {
        return this.visit(this.visit(ctx.condition).asBoolean(supplier(ctx)) ? ctx.left : ctx.right);
    }

    @Override
    public Value visitIsExpr(IsExprContext ctx) {
        return Value.valueOf(this.visit(ctx.expr(0)) == this.visit(ctx.expr(1)));
    }

    @Override
    public Value visitElvisExpr(ElvisExprContext ctx) {
        Value value = this.visit(ctx.expr(0));

        return value != Value.NULL ? value : this.visit(ctx.expr(1));
    }

    @Override
    public Value visitGen_mcfunction_line(Gen_mcfunction_lineContext ctx) {
        String current = namespaceStack.peek();
        Expr_blockContext expr_block = ctx.expr_block();
        String namespace = this.visit(expr_block.expr()).asString();
        if (namespace.startsWith("/")) {
            namespace = current + namespace;
        } else {
            if (!namespace.contains(":")) {
                int index = Math.max(current.indexOf(':'), current.indexOf('/')) + 1;
                namespace = current.substring(0, index) + namespace;
            }
        }
        if (!namespace.matches("[a-z_-][a-z\\d_-]*:[a-z_-][a-z\\d_-]*(/[a-z_-][a-z\\d_-]*)*"))
            throw new EvalException("Bad Function name: " + namespace + " sub: " + current, ctx);

        genFunction(namespace, expr_block.stat_block());

        return VOID;
    }

    private void genFunction(String namespace, Stat_blockContext ctx) {
        namespaceStack.push(namespace);
        enterScope();
        this.visit(ctx);
        exitScope();
        namespaceStack.pop();
    }

    @Override
    public Value visitCommand(CommandContext ctx) {
        StringBuilder sb = new StringBuilder();

        for (Command_partContext context : ctx.command_part()) {
            if (context instanceof CommandIdInterpPartContext) {                           //remove dollar sign
                String id = ((CommandIdInterpPartContext) context).ID_INTERP_COMMAND().getText().substring(1);
                sb.append(memory.getProperty(id).asString());
            } else if (context instanceof CommandExprInterpPartContext) {
                Value value = this.visit(((CommandExprInterpPartContext) context).expr());
                sb.append(value);
            } else if (context instanceof CommmandGoOnPartContext) {
                sb.append(' ');
            } else {
                sb.append(context.getText().replace("\\$", "$"));
            }
        }

        String current = namespaceStack.peek();
        if (ctx.OPEN_FUNCTION() != null) {
            String processed = ctx.expr() != null ? this.visit(ctx.expr()).asString() : "";
            String namespace;
            String extra;
            if (processed.length() == 0 || Character.isDigit(processed.charAt(0))) {
                int num = 1;
                do {
                    namespace = current + "/fun" + num++;
                } while (datapack.getNamespaces().contains(namespace));
                extra = processed;
            } else {
                int index = processed.indexOf(' ');
                if (index > -1) {
                    namespace = processed.substring(0, index);
                    extra = processed.substring(index + 1);
                } else {
                    namespace = processed;
                    extra = "";
                }
                if (namespace.startsWith("/")) {
                    namespace = current + namespace;
                } else if (!namespace.contains(":")) {
                    index = Math.max(current.indexOf(':'), current.lastIndexOf('/')) + 1;
                    namespace = current.substring(0, index) + namespace;
                }
            }
            if (!namespace.matches("[a-z_-][a-z\\d_-]*:[a-z_-][a-z\\d_-]*(/[a-z_-][a-z\\d_-]*)*"))
                throw new EvalException("Bad Function name: namespace: " + namespace + " sub: " + current, ctx);
            sb.append("function ");
            sb.append(namespace);
            if (extra.length() > 0) {
                sb.append(' ').append(extra);
            }
            genFunction(namespace, ctx.stat_block());
        }


        if (ctx.OPEN_COMMAND() != null) { // make blocks of commands in the functions reflect in the mcfunction file
            datapack.addLine(current, "");
        }

        String string = sb.toString();
        datapack.addLine(current, string);
        return VOID;
    }
}
