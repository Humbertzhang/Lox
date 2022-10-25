package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
    // 函数元数，返回函数应当被传入几个参数
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}