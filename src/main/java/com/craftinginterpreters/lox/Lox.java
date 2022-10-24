package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Lox {
    // 新增部分开始
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    static boolean repl = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }

    }

    // 以文件形式运行
    private static void runFile(String path) throws IOException {
        repl = false;

        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    // 以交互式形式运行
    private static void runPrompt() throws IOException {
        // my code for enhancing repl mode
        repl = true;

        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    // 运行(行 or 文件)
    private static void run(String source) {
        // 词法分析, 由扫描器Scanner完成，产生一个个词token
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // 语法分析, 由Parser完成, 产生statements, 其中一个Statement可能由一个或多个expression组成
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        if (!repl) {
            // 文件mod运行
            interpreter.interpret(statements);
        } else {
            // REPL mode 运行, 这里，，否则只运行
            for (Stmt statement: statements) {
                // 如果是expression, 那么就直接打印结果
                if (statement instanceof Stmt.Expression exprStatement) {
                    interpreter.interpret(exprStatement.expression);
                } else {
                    // 否则只执行statement，不打印
                    List<Stmt> tempStatementList = new ArrayList<>();
                    tempStatementList.add(statement);
                    interpreter.interpret(tempStatementList);
                }
            }
        }

    }

    // 错误处理
    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where,
                               String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    // parse error
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

}