package com.craftinginterpreters.lox;

// 第五章.习题3 打印Expr的逆波兰式
public class RPNVisitor implements Expr.Visitor<String> {
    String printRPN(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return rpn(expr.operator.lexeme,
                expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return rpn("", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return rpn(expr.operator.lexeme, expr.right);
    }

    private String rpn(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        for (Expr expr: exprs) {
            builder.append(expr.accept(this));
            builder.append(" ");
        }
        builder.append(name);

        return builder.toString();
    }

    public static void main(String[] args) {
        // -123 * (45.67)
//        Expr expression = new Expr.Binary(
//                new Expr.Unary(
//                        new Token(TokenType.MINUS, "-", null, 1),
//                        new Expr.Literal(123)),
//                new Token(TokenType.STAR, "*", null, 1),
//                new Expr.Grouping(new Expr.Literal(45.67)));

        // (1 + 2) * (4 - 3)
        Expr expression = new Expr.Binary(
                    new Expr.Binary(new Expr.Literal(1), new Token(TokenType.PLUS, "+", null, 1), new Expr.Literal(2)),
                    new Token(TokenType.STAR, "*", null, 1),
                    new Expr.Binary(new Expr.Literal(4), new Token(TokenType.MINUS, "-", null, 1), new Expr.Literal(3))
                );

        System.out.println(new RPNVisitor().printRPN(expression));
    }
}
