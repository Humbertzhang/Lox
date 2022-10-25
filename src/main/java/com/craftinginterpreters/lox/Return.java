package com.craftinginterpreters.lox;

class Return extends RuntimeException {
    final Object value;

    // 使用value来携带返回值
    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
