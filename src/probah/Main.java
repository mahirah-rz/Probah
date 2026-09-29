package probah;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import probah.ast.AstPrinter;
import probah.ast.ProgramNode;
import probah.lexer.Lexer;
import probah.lexer.Token;
import probah.parser.Parser;
import probah.semantic.SemanticAnalyzer;
import probah.symbol.SymbolTable;
import probah.tac.TACInstruction;
import probah.tac.TACOptimizer;
import probah.tac.TACgenerator;
import probah.tac.JavaCodeGenerator;

public class Main {


public static void main(String[] args) {

    if (args.length != 1) {
        System.out.println("Usage: java -cp out probah.Main <source-file.probah>");
        return;
    }

    String fileName = args[0];

    try {
        Path sourcePath = Path.of(fileName);

        String source = Files.readString(
                sourcePath,
                StandardCharsets.UTF_8
        );

        System.out.println("PROBAH COMPILER");
        System.out.println("Source file: " + fileName);
        System.out.println();

        System.out.println("TOKENS");

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            System.out.println(token);
        }

        if (!lexer.getErrors().isEmpty()) {
            System.out.println();
            System.out.println("LEXICAL ERRORS");

            for (String error : lexer.getErrors()) {
                System.out.println(error);
            }

            System.out.println();
            System.out.println("Compilation stopped because of lexical errors.");
            return;
        }

        System.out.println();
        System.out.println("Lexical analysis completed successfully.");

        System.out.println();
        System.out.println("PARSING");

        Parser parser = new Parser(tokens);
        ProgramNode program = parser.parse();

        if (!parser.getErrors().isEmpty()) {
            System.out.println();
            System.out.println("SYNTAX ERRORS");

            for (String error : parser.getErrors()) {
                System.out.println(error);
            }

            System.out.println();
            System.out.println("Compilation stopped because of syntax errors.");
            return;
        }

        System.out.println("Parsing completed successfully.");

        System.out.println();
        System.out.println("AST:");
        AstPrinter.print(program);

        System.out.println();
        System.out.println("SEMANTIC ANALYSIS:");

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        SymbolTable symbolTable = semanticAnalyzer.analyze(program);

        if (!semanticAnalyzer.getErrors().isEmpty()) {

            for (String error : semanticAnalyzer.getErrors()) {
                System.out.println(error);
            }

            System.out.println();
            System.out.println("Compilation stopped: semantic errors found.");
            return;
        }

        System.out.println("No semantic errors.");

        System.out.println();
        System.out.println("SYMBOL TABLE");

        symbolTable.printTable();

        System.out.println();
        System.out.println("TAC:");

        TACgenerator tacGenerator = new TACgenerator();
        List<TACInstruction> tac = tacGenerator.generate(program);

        int number = 1;

        for (TACInstruction instruction : tac) {
            System.out.printf(
                    "%3d: %s%n",
                    number++,
                    instruction
            );
        }

        TACOptimizer optimizer = new TACOptimizer();
        List<TACInstruction> optimizedTac = optimizer.optimize(tac);

        System.out.println();
        System.out.println("OPTIMIZED TAC:");

        number = 1;

        for (TACInstruction instruction : optimizedTac) {
            System.out.printf(
                    "%3d: %s%n",
                    number++,
                    instruction
            );
        }

        JavaCodeGenerator javaGenerator = new JavaCodeGenerator();

        String className = createClassName(sourcePath);

        String javaSource = javaGenerator.generate(
                program,
                className
        );

        Path outputDirectory = Path.of("output");

        Files.createDirectories(outputDirectory);

        Path javaFile = outputDirectory.resolve(
                className + ".java"
        );

        Files.writeString(
                javaFile,
                javaSource,
                StandardCharsets.UTF_8
        );

        System.out.println();
        System.out.println("JAVA TARGET:");
        System.out.println(javaSource);

        System.out.println();
        System.out.println("Generated Java file:");
        System.out.println(javaFile.toAbsolutePath());

        System.out.println();
        System.out.println("Compilation completed!");

    } catch (IOException e) {

        System.out.println(
                "I/O Error: " + e.getMessage()
        );

    } catch (RuntimeException e) {

        System.out.println(
                "Compiler Error: " + e.getMessage()
        );
    }
}

private static String createClassName(Path sourcePath) {

    String filename = sourcePath.getFileName().toString();

    if (filename.endsWith(".probah")) {
        filename = filename.substring(
                0,
                filename.length() - 7
        );
    }

    StringBuilder result = new StringBuilder();

    boolean capitalizeNext = true;

    for (char c : filename.toCharArray()) {

        if (!Character.isLetterOrDigit(c)) {
            capitalizeNext = true;
            continue;
        }

        if (capitalizeNext) {
            result.append(Character.toUpperCase(c));
            capitalizeNext = false;
        } else {
            result.append(c);
        }
    }

    if (result.length() == 0) {
        return "GeneratedProbahProgram";
    }

    if (!Character.isJavaIdentifierStart(result.charAt(0))) {
        result.insert(0, "Generated");
    }

    return result.toString();
}

}
