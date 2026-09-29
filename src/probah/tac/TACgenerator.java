package probah.tac;

import probah.ast.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TACgenerator {

    private final List<TACInstruction> instructions =
            new ArrayList<>();

    private int tempCount = 1;

    private int labelCount = 1;

    private int scopeCount = 1;

    private final Deque<Map<String, String>> scopes =
            new ArrayDeque<>();


    public List<TACInstruction> generate(
            ProgramNode program) {

        instructions.clear();

        tempCount = 1;

        labelCount = 1;

        scopeCount = 1;

        scopes.clear();

        enterScope("global");

        for (StatementNode statement :
                program.getStatements()) {

            generateStatement(statement);
        }

        exitScope();

        return Collections.unmodifiableList(
                instructions
        );
    }


    private void generateStatement(
            StatementNode node) {

        if (node instanceof DeclarationNode declaration) {

            String value =
                    generateExpression(
                            declaration.getInitializer()
                    );

            String scopedName =
                    declareVariable(
                            declaration.getName()
                    );

            instructions.add(
                    new TACInstruction(
                            scopedName
                                    + " = "
                                    + value
                    )
            );
        }


        else if (node instanceof AssignmentNode assignment) {

            String value =
                    generateExpression(
                            assignment.getValue()
                    );

            String scopedName =
                    resolveVariable(
                            assignment.getName()
                    );

            instructions.add(
                    new TACInstruction(
                            scopedName
                                    + " = "
                                    + value
                    )
            );
        }


        else if (node instanceof PrintNode print) {

            String value =
                    generateExpression(
                            print.getExpression()
                    );

            instructions.add(
                    new TACInstruction(
                            "print " + value
                    )
            );
        }


        else if (node instanceof BlockNode block) {

            String scopeName =
                    "block-" + scopeCount++;

            enterScope(scopeName);

            for (StatementNode statement :
                    block.getStatements()) {

                generateStatement(statement);
            }

            exitScope();
        }


        else if (node instanceof IfNode ifNode) {

            generateIf(ifNode);
        }


        else if (node instanceof WhileNode whileNode) {

            generateWhile(whileNode);
        }
    }


    private void generateIf(
            IfNode node) {

        String condition =
                generateExpression(
                        node.getCondition()
                );

        String elseLabel =
                newLabel();

        String endLabel =
                newLabel();

        instructions.add(
                new TACInstruction(
                        "ifFalse "
                                + condition
                                + " goto "
                                + elseLabel
                )
        );

        generateStatement(
                node.getThenBranch()
        );

        if (node.getElseBranch() != null) {

            instructions.add(
                    new TACInstruction(
                            "goto "
                                    + endLabel
                    )
            );

            instructions.add(
                    new TACInstruction(
                            elseLabel + ":"
                    )
            );

            generateStatement(
                    node.getElseBranch()
            );

            instructions.add(
                    new TACInstruction(
                            endLabel + ":"
                    )
            );

        } else {

            instructions.add(
                    new TACInstruction(
                            elseLabel + ":"
                    )
            );
        }
    }


    private void generateWhile(
            WhileNode node) {

        String startLabel =
                newLabel();

        String endLabel =
                newLabel();

        instructions.add(
                new TACInstruction(
                        startLabel + ":"
                )
        );

        String condition =
                generateExpression(
                        node.getCondition()
                );

        instructions.add(
                new TACInstruction(
                        "ifFalse "
                                + condition
                                + " goto "
                                + endLabel
                )
        );

        generateStatement(
                node.getBody()
        );

        instructions.add(
                new TACInstruction(
                        "goto "
                                + startLabel
        )
        );

        instructions.add(
                new TACInstruction(
                        endLabel + ":"
                )
        );
    }


    private String generateExpression(
            ExpressionNode node) {

        if (node instanceof IntegerLiteralNode integer) {

            return Integer.toString(
                    integer.getValue()
            );
        }


        if (node instanceof BooleanLiteralNode bool) {

            return Boolean.toString(
                    bool.getValue()
            );
        }


        if (node instanceof IdentifierNode identifier) {

            return resolveVariable(
                    identifier.getName()
            );
        }


        if (node instanceof UnaryExpressionNode unary) {

            String operand =
                    generateExpression(
                            unary.getOperand()
                    );

            String temp =
                    newTemp();

            instructions.add(
                    new TACInstruction(
                            temp
                                    + " = "
                                    + unary.getOperator().getLexeme()
                                    + " "
                                    + operand
                    )
            );

            return temp;
        }


        if (node instanceof BinaryExpressionNode binary) {

            String left =
                    generateExpression(
                            binary.getLeft()
                    );

            String right =
                    generateExpression(
                            binary.getRight()
                    );

            String temp =
                    newTemp();

            instructions.add(
                    new TACInstruction(
                            temp
                                    + " = "
                                    + left
                                    + " "
                                    + binary.getOperator().getLexeme()
                                    + " "
                                    + right
                    )
            );

            return temp;
        }


        throw new IllegalArgumentException(
                "Unknown expression node: "
                        + node.getClass()
                        .getSimpleName()
        );
    }


    private void enterScope(
            String scopeName) {

        scopes.push(
                new HashMap<>()
        );
    }


    private void exitScope() {

        scopes.pop();
    }


    private String declareVariable(
            String name) {

        String scopeName;

        if (scopes.size() == 1) {
            scopeName = "global";
        } else {
            scopeName =
                    "block-" + (scopeCount - 1);
        }

        String scopedName =
                name + "@" + scopeName;

        scopes.peek().put(
                name,
                scopedName
        );

        return scopedName;
    }


    private String resolveVariable(
            String name) {

        for (Map<String, String> scope :
                scopes) {

            String scopedName =
                    scope.get(name);

            if (scopedName != null) {
                return scopedName;
            }
        }

        return name;
    }


    private String newTemp() {

        return "t" + tempCount++;
    }


    private String newLabel() {

        return "L" + labelCount++;
    }
}