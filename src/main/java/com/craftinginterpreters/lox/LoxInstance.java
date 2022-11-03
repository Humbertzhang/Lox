package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass klass;

    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        // get 属性
        if (fields.containsKey(name.lexeme)) {
            return this.fields.get(name.lexeme);
        }

        // get 方法
        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) {
            // 将方法与this关键字对应的LoxInstance绑定
            return method.bind(this);
        }

        // 发现instance没有的属性、方法，抛出运行时异常
        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
