package com.craftinginterpreters.lox;

// Token这个类本身. 记录一个Token的各类信息
// 包括类型, String, 字面量?常量, 行号等
class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}