/*
expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;

http://www.craftinginterpreters.com/parsing-expressions.html#the-parser-class
A recursive descent parser is a literal translation of the grammar’s rules straight into imperative code. Each rule becomes a function. The body of the rule translates to code roughly like:
Grammar notation	Code representation
Terminal	        Code to match and consume a token
Nonterminal	        Call to that rule’s function
|	                if or switch statement
* or +	            while or for loop
?	                if statement


*/


package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

// 语法分析器
public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    // declaration    → varDecl | statement ;
    // 任何允许声明的地方都允许一个非声明式的语句，所以 declaration 规则会下降到statement
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            // 如果匹配varDeclaration, 那么就为声明语句，否则递归下降到statement语句.
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(FOR)) {
            return forStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        // {, 块作用域开始
        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        return expressionStatement();
    }

    // for (initializer; condition; increment) {}
    // for (int i=0; i<10; i= i + 1) {}
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // for语句初始化式子 initializer
        Stmt initializer;
        // 直接是分号，代表没有初始化式子 如: for (; i < 10; i = i+1)
        if (match(SEMICOLON)) {
            initializer = null;
        // 变量声明，代表是初始化式子, 且该变量作用域仅在该for循环中
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
        // 普通表达式 如 for (i=10; i < 20; i=i+1) {}
            initializer = expressionStatement();
        }

        // condition语句
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // increment语句
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // body体
        Stmt body = statement();

        // 语法脱糖, 使用现有表达式来表达for循环

        // 将increment加入到body的最后
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        // 使用while来处理循环condition
        if (condition == null) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        // 处理初始化语句，先执行该初始化语句再执行while循环
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        // 左括号
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        // 逻辑表达式
        Expr condition = expression();
        // 右括号
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        // else与前面最近的if绑定在一起, 避免悬空else问题，即不知道else与之前哪个if对应.
        if (match(ELSE)) {
            elseBranch = statement();
        }

        // 解析成功if语句
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    // varDeclaration statement
    private Stmt varDeclaration() {
        // 检查下一个Token是否为IDENTIFIER
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        // 如果下一个token为 =，那么代表该变量有初始化函数
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        // 在parse过程中确定该Statement的类型为Var Statement
        // 并设置该Statement的name(变量名) 与 初始化函数(initializer)
        // 该初始化函数为什么就会最终返回什么Expr
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        // 左括号
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        // 逻辑表达式
        Expr condition = expression();
        // 右括号
        consume(RIGHT_PAREN, "Expect ')' after while condition.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    // statement vs Expression
    // Expression(表达式): produce at least one value
    // Statement(语句) Do Something and are often composed of expressions (or other statements)
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // 块作用域
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        // 在出现右花括号之前为同一个作用域
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    // 赋值表达式(a = 1，而不是 var a = 1)
    private Expr assignment() {
        Expr expr = or();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        // 第一个expr会先递归下降comparison,
        // comparison会随之递归下降term等
        // 直到结束所有递归下降
        Expr expr = comparison();

        // 当找到了 != 或 == 时，视为匹配到了一个equality expression
        // 继续递归下降
        // 这里之所以是while是为了处理 1 == 2 == 3 这种表达式, 可以一直处理 == 或 != , 而不是只能处理 1 == 2
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        // 否则直接返回一开始的expr，其为一个comparison expression, 且会递归下降
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        // 解析变量表达式
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            // TODO: Syntax Errors
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // 当前token不符合primary的case，抛出异常
        throw error(peek(), "Exception expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // 判断当前字符是否符合预期，否则抛出异常
    private Token consume(TokenType type, String message) {
        // 当前type符合预期，那么继续处理
        if (check(type)) {
            return advance();
        }
        // 否则抛出异常(panic 模式)
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    // consumes the current token and returns it
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    // 返回current指向的当前的token
    private Token peek() {
        return tokens.get(current);
    }

    // 返回current的前一个token
    private Token previous() {
        return tokens.get(current-1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    // 抛出错误后同步
    // 丢弃标记，直至达到下一条语句的开头
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
