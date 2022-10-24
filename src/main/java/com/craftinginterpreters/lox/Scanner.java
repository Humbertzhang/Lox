/*
* 词法分析 Scanner
* */
package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

// 语法分析
public class Scanner {
    // 源代码字符串
    private final String source;
    // 生产出来的Token
    private final List<Token> tokens = new ArrayList<>();

    // start字段指向被扫描的词素中的第一个字符
    private int start = 0;
    // current字段指向当前正在处理的字符
    private int current = 0;
    // The line field tracks what source line current is on,
    // so we can produce tokens that know their location
    private int line = 1;

    // 保留字
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        // 结束扫描
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    // 读取下一个Token
    private void scanToken() {
        // 获取一个字符输入
        char c = advance();

        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            // 针对! 与 !=
            case '!':
                addToken(match('=') ? BANG_EQUAL: BANG);
                break;
            // 针对 == 与 =
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            // 针对 <= 与 <
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            // 针对 >= 与 >
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            // 针对 除法 / 与 注释 //
            case '/':
                if (match('/')) {
                    // 注释
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    blockComments();
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            case '"':
                // 扫描字符串常量，并addToken
                string();
                break;
            default:
                // 数字
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    // 标识符或保留字
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = IDENTIFIER;
        }
        addToken(type);
    }

    // 数字常量
    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // 字符串常量
    private void string() {
        // 一直消费字符，直到遇到结束的"
        while (peek() != '"' && !isAtEnd()) {
            // 支持多行字符
            if (peek() == '\n') line++;
            advance();
        }

        // 未关闭的string
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }
        // 消费结束的"
        advance();

        String value = source.substring(start+1, current-1);
        addToken(STRING, value);
    }

    // 暂时不支持内嵌的块状注释, 即 /*... /* ... */  ...*/
    private void blockComments() {
        while (peek() != '*' && peekNext() != '/' && !isAtEnd() && !nextAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        // 未关闭的 block comment
        if (isAtEnd()) {
            Lox.error(line, "Unterminated block comments.");
            return;
        }

        // 未关闭的 block comment 情况2: 处理 /* *  的问题，即末尾缺少一个/
        if (nextAtEnd()) {
            // consume '*'
            advance();
            Lox.error(line, "Unterminated block comments.");
            return;
        }

        // 消费结束的 */
        advance();
        advance();
    }

    // 检查下一个字符是否符合预期，符合则会current+1，并产生一个token.
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }

    // It only looks at the current unconsumed character
    // This is called lookahead.
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // 处理小数点的数字需要两个前瞻(lookahead)字符.
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // 是字符
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    // 是字符或数字
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // The advance() method consumes the next character in the source file and returns it.
    // 用于输入
    private char advance() {
        current+=1;
        return source.charAt(current-1);
    }

    // 用于输出
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean nextAtEnd() {
        return current+1 >= source.length();
    }
}
