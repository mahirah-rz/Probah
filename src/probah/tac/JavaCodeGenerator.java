package probah.tac;

import probah.ast.AssignmentNode;
import probah.ast.BinaryExpressionNode;
import probah.ast.BlockNode;
import probah.ast.BooleanLiteralNode;
import probah.ast.DeclarationNode;
import probah.ast.ExpressionNode;
import probah.ast.IdentifierNode;
import probah.ast.IfNode;
import probah.ast.IntegerLiteralNode;
import probah.ast.PrintNode;
import probah.ast.ProgramNode;
import probah.ast.StatementNode;
import probah.ast.UnaryExpressionNode;
import probah.ast.WhileNode;
import probah.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class JavaCodeGenerator {


private final List<String> lines = new ArrayList<>();

private int indentLevel = 0;

public String generate(
        ProgramNode program,
        String className) {

    lines.clear();
    indentLevel = 0;

    if (className == null || className.isBlank()) {
        className = "GeneratedProbahProgram";
    }

    addLine("public class " + className + " {");

    indentLevel++;

    addLine("public static void main(String[] args) {");

    indentLevel++;

    for (StatementNode statement : program.getStatements()) {
        generateStatement(statement);
    }

    indentLevel--;

    addLine("}");

    indentLevel--;

    addLine("}");

    return String.join(
            System.lineSeparator(),
            lines
    );
}

private void generateStatement(StatementNode statement) {

    if (statement instanceof DeclarationNode) {

        generateDeclaration(
                (DeclarationNode) statement
        );

    } else if (statement instanceof AssignmentNode) {

        generateAssignment(
                (AssignmentNode) statement
        );

    } else if (statement instanceof PrintNode) {

        generatePrint(
                (PrintNode) statement
        );

    } else if (statement instanceof IfNode) {

        generateIf(
                (IfNode) statement
        );

    } else if (statement instanceof WhileNode) {

        generateWhile(
                (WhileNode) statement
        );

    } else if (statement instanceof BlockNode) {

        generateBlock(
                (BlockNode) statement
        );
    }
}

private void generateDeclaration(
        DeclarationNode node) {

    String javaType =
            getJavaType(
                    node.getDeclaredType()
            );

    String name =
            node.getName();

    String initializer =
            generateExpression(
                    node.getInitializer()
            );

    addLine(
            javaType
                    + " "
                    + name
                    + " = "
                    + initializer
                    + ";"
    );
}

private void generateAssignment(
        AssignmentNode node) {

    String name =
            node.getName();

    String expression =
            generateExpression(
                    node.getValue()
            );

    addLine(
            name
                    + " = "
                    + expression
                    + ";"
    );
}

private void generatePrint(
        PrintNode node) {

    String expression =
            generateExpression(
                    node.getExpression()
            );

    addLine(
            "System.out.println("
                    + expression
                    + ");"
    );
}

private void generateIf(
        IfNode node) {

    String condition =
            generateExpression(
                    node.getCondition()
            );

    addLine(
            "if (" + condition + ") {"
    );

    indentLevel++;

    generateStatement(
            node.getThenBranch()
    );

    indentLevel--;

    if (node.getElseBranch() != null) {

        addLine("} else {");

        indentLevel++;

        generateStatement(
                node.getElseBranch()
        );

        indentLevel--;
    }

    addLine("}");
}

private void generateWhile(
        WhileNode node) {

    String condition =
            generateExpression(
                    node.getCondition()
            );

    addLine(
            "while (" + condition + ") {"
    );

    indentLevel++;

    generateStatement(
            node.getBody()
    );

    indentLevel--;

    addLine("}");
}

private void generateBlock(
        BlockNode node) {

    for (StatementNode statement :
            node.getStatements()) {

        generateStatement(statement);
    }
}

private String generateExpression(
        ExpressionNode expression) {

    if (expression instanceof IntegerLiteralNode) {

        IntegerLiteralNode node =
                (IntegerLiteralNode) expression;

        return Integer.toString(
                node.getValue()
        );
    }

    if (expression instanceof BooleanLiteralNode) {

        BooleanLiteralNode node =
                (BooleanLiteralNode) expression;

        return Boolean.toString(
                node.getValue()
        );
    }

    if (expression instanceof IdentifierNode) {

        IdentifierNode node =
                (IdentifierNode) expression;

        return node.getName();
    }

    if (expression instanceof UnaryExpressionNode) {

        return generateUnary(
                (UnaryExpressionNode) expression
        );
    }

    if (expression instanceof BinaryExpressionNode) {

        return generateBinary(
                (BinaryExpressionNode) expression
        );
    }

    return "/* unknown expression */";
}

private String generateUnary(
        UnaryExpressionNode node) {

    String operand =
            generateExpression(
                    node.getOperand()
            );

    TokenType operator =
            node.getOperator().getType();

    if (operator == TokenType.NOT) {
        return "!(" + operand + ")";
    }

    if (operator == TokenType.MINUS) {
        return "-(" + operand + ")";
    }

    return operand;
}

private String generateBinary(
        BinaryExpressionNode node) {

    String left =
            generateExpression(
                    node.getLeft()
            );

    String right =
            generateExpression(
                    node.getRight()
            );

    String operator =
            getJavaOperator(
                    node.getOperator().getType(),
                    node.getOperator().getLexeme()
            );

    return "("
            + left
            + " "
            + operator
            + " "
            + right
            + ")";
}

private String getJavaOperator(
        TokenType type,
        String lexeme) {

    switch (type) {

        case PLUS:
            return "+";

        case MINUS:
            return "-";

        case MULTIPLY:
            return "*";

        case DIVIDE:
            return "/";

        case LESS:
            return "<";

        case LESS_EQUAL:
            return "<=";

        case GREATER:
            return ">";

        case GREATER_EQUAL:
            return ">=";

        case EQUAL_EQUAL:
            return "==";

        case NOT_EQUAL:
            return "!=";

        case AND:
            return "&&";

        case OR:
            return "||";

        default:
            return lexeme;
    }
}

private String getJavaType(TokenType type) {

    if (type == TokenType.INTEGER_TYPE) {
        return "int";
    }

    if (type == TokenType.BOOLEAN_TYPE) {
        return "boolean";
    }

    return "Object";
}

private void addLine(String line) {

    StringBuilder builder =
            new StringBuilder();

    for (int i = 0;
         i < indentLevel;
         i++) {

        builder.append("    ");
    }

    builder.append(line);

    lines.add(builder.toString());
}


}
