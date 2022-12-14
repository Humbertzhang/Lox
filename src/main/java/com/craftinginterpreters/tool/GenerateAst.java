package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        String outputDir = "./src/main/java/com/craftinginterpreters/lox";
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                // 函数调用表达式
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                // 实例属性访问
                "Get      : Expr object, Token name",
                "Grouping : Expr expression",
                "Literal  : Object value",
                // 逻辑运算符 and or
                "Logical  : Expr left, Token operator, Expr right",
                // 实例属性设置
                "Set      : Expr object, Token name, Expr value",
                // Super 关键字
                "Super    : Token keyword, Token method",
                // 支持class的this关键字
                "This     : Token keyword",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                // 块作用域
                "Block      : List<Stmt> statements",
                "Break      : ",
                "Class      : Token name, Expr.Variable superclass, List<Stmt.Function> methods",
                "Expression : Expr expression",
                // 函数声明Statement
                "Function   : Token name, List<Token> params," + " List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch," + " Stmt elseBranch",
                "Print      : Expr expression",
                // Token keyword 用于记录 return 的位置
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        // 用于写文件
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        writer.write("\n");
        // 生成访问者，用于处理各个类型的表达式
        defineVisitor(writer, baseName, types);


        // 生成AST类型
        for(String type: types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
            writer.write("\n");
        }

        // The base accept() method.
        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");
        // 遍历所有的子类，并为每个子类声明一个visit方法
        for (String type: types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("  }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("  static class " + className + " extends " + baseName + " {");
        // Constructor.
        writer.println("    " + className + "(" + fieldList + ") {");

        String[] fields;
        if (fieldList.isEmpty()) {
            fields = new String[0];
        } else {
            fields = fieldList.split(", ");
        }

        for (String field: fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }
        writer.println("    }");

        // 每个类型生成对应的accept函数，用于处理visitor
        // Visitor pattern.
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" +
                className + baseName + "(this);");
        writer.println("    }");


        // Fields.
        writer.println();
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }
        writer.println("  }");
    }
}
