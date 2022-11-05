package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    // 闭包环境
    private final Environment closure;

    // 是否是类的初始化函数
    private final boolean isInitializer;

    private final boolean isGetterFunc = false;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
    }

    // 将类的方法与具体的实例绑定
    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        // 当catch到return后，获得返回值并返回
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            // 在初始化函数中返回（仅能空返回，因为非空返回已经在resolver中被处理了）
            // 会直接返回this instance.
            if (isInitializer) return closure.getAt(0, "this");

            return returnValue.value;
        }

        // 如果为初始化函数直接该实例
        if (isInitializer) {
            return closure.getAt(0, "this");
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
