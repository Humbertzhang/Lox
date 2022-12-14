package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

// 解析器代码
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    // 跟踪当前函数状态
    private FunctionType currentFunction = FunctionType.NONE;
    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        // 初始化函数
        INITIALIZER,
    }
    private enum ClassType {
        NONE,
        CLASS,
        // 是否在一个子类中
        SUBCLASS
    }

    // 遍历语法树时，当前是否在一个类声明中
    private ClassType currentClass = ClassType.CLASS;

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        // 记录当前以及前一个的FunctionType状态
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param: function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    // 创建Resolver新作用域
    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    // 结束当前Resolver作用域
    private void endScope() {
        scopes.pop();
    }

    // 将变量分为两个步骤，变量声明与定义
    // 此处为声明
    private void declare(Token name) {
        if (scopes.empty()) {
            return;
        }
        Map<String, Boolean> scope = scopes.peek();

        // 以下代码处理同名变量不允许多次声明，如 {var a = 1; var a = 2;}
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,  "Already variable with this name in this scope.");
        }

        // false代表仅声明了，但是还没初始化
        scope.put(name.lexeme, false);
    }

    // 此处为变量定义，也即完成初始化了(即便没有initializer, 也会被初始化为null, 所以没有initializer也会调用define)
    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        scopes.peek().put(name.lexeme, true);
    }

    // 如果我们能够保证变量查找总是在环境链上遍历相同数量的链接，
    // 也就可以保证每次都可以在相同的作用域中找到相同的变量。
    private void resolveLocal(Expr expr, Token name) {
        for(int i = scopes.size()-1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                // 传入当前最内层作用域和变量所在作用域之间的数量.
                // 如果变量在当前作用域中找到该变量，则传入0；如果在紧邻的外网作用域中找到，则传1, 以此类推
                interpreter.resolve(expr, scopes.size()-1-i);
                return;
            }
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        // 保持上一个currentClass状态
        ClassType enclosingClass = currentClass;
        // 更新 currentClass 状态为Class
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        // 子类与父类不能相同
        if (stmt.superclass != null &&
            stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
            Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
        }

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }

        // 开始超类作用域，为会使用的super添加true变量
        if (stmt.superclass != null) {
            beginScope();
            scopes.peek().put("super", true);
        }

        // 设置this的作用域
        beginScope();
        scopes.peek().put("this", true);

        for(Stmt.Function method: stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }

        endScope();

        // 结束超类作用域
        if (stmt.superclass != null) {
            endScope();
        }

        // 恢复之前的class状态
        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        // 语义分析：防止在函数外进行return
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            // 静态分析：类的INIT函数不能返回值
            if (currentClass == ClassType.CLASS && currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword,
                        "Can't return a value from an initializer.");
            }

            resolve(stmt.value);
        }

        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        /*
        var a = "outer";
        {var a = a;}
        处理此种在初始化式子中使用了被初始化的变量的情况
        * */
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    // During resolution, we recurse only into the expression to the left of the dot
    // The actual property access happens in the interpreter.
    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
//        System.out.println("resolver:" + expr.value);
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
        }
        else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        // 静态分析，防止this在类之外被使用
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }



}
