package probah.semantic;

import probah.ast.*;
import probah.lexer.TokenType;
import probah.symbol.Symbol;
import probah.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable =
            new SymbolTable();

    private final List<String> errors =
            new ArrayList<>();


    // =====================================================
    // START SEMANTIC ANALYSIS
    // =====================================================

    public SymbolTable analyze(
            ProgramNode program) {

        for (StatementNode statement :
                program.getStatements()) {

            visitStatement(statement);
        }

        return symbolTable;
    }


    // =====================================================
    // GET ERRORS
    // =====================================================

    public List<String> getErrors() {

        return Collections.unmodifiableList(
                errors
        );
    }


    // =====================================================
    // STATEMENT VISITOR
    // =====================================================

    private void visitStatement(
            StatementNode node) {

        if (node instanceof DeclarationNode declaration) {

            visitDeclaration(declaration);

        } else if (node instanceof AssignmentNode assignment) {

            visitAssignment(assignment);

        } else if (node instanceof PrintNode print) {

            visitExpression(
                    print.getExpression()
            );

        } else if (node instanceof IfNode ifNode) {

            visitIf(ifNode);

        } else if (node instanceof WhileNode whileNode) {

            visitWhile(whileNode);

        } else if (node instanceof BlockNode block) {

            visitBlock(block);
        }
    }


    // =====================================================
    // DECLARATION
    // =====================================================

    private void visitDeclaration(
            DeclarationNode node) {

        TokenType declared =
                node.getDeclaredType();


        // Only INTEGER and BOOLEAN
        // are allowed.
        if (declared != TokenType.INTEGER_TYPE
                && declared != TokenType.BOOLEAN_TYPE) {

            error(
                    node.getLine(),
                    "Unknown declaration type for '"
                            + node.getName()
                            + "'."
            );

            return;
        }


        // Check duplicate declaration
        // in current scope.
        if (!symbolTable.define(
                node.getName(),
                declared,
                node.getLine())) {

            error(
                    node.getLine(),
                    "Variable '"
                            + node.getName()
                            + "' is already declared in this scope."
            );

            // Still analyze initializer.
            visitExpression(
                    node.getInitializer()
            );

            return;
        }


        SemanticType expected;

        if (declared == TokenType.INTEGER_TYPE) {

            expected =
                    SemanticType.INTEGER;

        } else {

            expected =
                    SemanticType.BOOLEAN;
        }


        // Determine initializer type.
        SemanticType actual =
                visitExpression(
                        node.getInitializer()
                );


        // Type mismatch.
        if (actual != SemanticType.ERROR
                && actual != expected) {

            error(
                    node.getLine(),
                    "Type mismatch: '"
                            + node.getName()
                            + "' is "
                            + expected
                            + " but initializer is "
                            + actual
                            + "."
            );

        }

        // Correct type.
        else if (actual == expected) {

            symbolTable.update(
                    node.getName(),
                    "initialized"
            );
        }
    }


    // =====================================================
    // ASSIGNMENT
    // =====================================================

    private void visitAssignment(
            AssignmentNode node) {

        Symbol symbol =
                symbolTable.lookup(
                        node.getName()
                );


        // Analyze RHS.
        SemanticType actual =
                visitExpression(
                        node.getValue()
                );


        // Variable not declared.
        if (symbol == null) {

            error(
                    node.getLine(),
                    "Variable '"
                            + node.getName()
                            + "' used before declaration."
            );

            return;
        }


        SemanticType expected;

        if (symbol.getType()
                == TokenType.INTEGER_TYPE) {

            expected =
                    SemanticType.INTEGER;

        } else {

            expected =
                    SemanticType.BOOLEAN;
        }


        // Assignment type mismatch.
        if (actual != SemanticType.ERROR
                && actual != expected) {

            error(
                    node.getLine(),
                    "Type mismatch: cannot assign "
                            + actual
                            + " to "
                            + expected
                            + " variable '"
                            + node.getName()
                            + "'."
            );

            return;
        }


        // Correct assignment.
        if (actual == expected) {

            symbolTable.update(
                    node.getName(),
                    "initialized"
            );
        }
    }


    // =====================================================
    // IF
    // =====================================================

    private void visitIf(
            IfNode node) {

        SemanticType condition =
                visitExpression(
                        node.getCondition()
                );


        // IF condition must be Boolean.
        if (condition != SemanticType.ERROR
                && condition != SemanticType.BOOLEAN) {

            error(
                    node.getCondition().getLine(),
                    "IF condition must be Boolean."
            );
        }


        // THEN block
        visitBlock(
                node.getThenBranch()
        );


        // ELSE
        if (node.getElseBranch() != null) {

            // ELSE IF
            if (node.getElseBranch()
                    instanceof IfNode nestedIf) {

                visitIf(nestedIf);

            }

            // ELSE block
            else if (node.getElseBranch()
                    instanceof BlockNode block) {

                visitBlock(block);
            }
        }
    }


    // =====================================================
    // WHILE
    // =====================================================

    private void visitWhile(
            WhileNode node) {

        SemanticType condition =
                visitExpression(
                        node.getCondition()
                );


        // WHILE condition must be Boolean.
        if (condition != SemanticType.ERROR
                && condition != SemanticType.BOOLEAN) {

            error(
                    node.getCondition().getLine(),
                    "WHILE condition must be Boolean."
            );
        }


        visitBlock(
                node.getBody()
        );
    }


    // =====================================================
    // BLOCK
    // =====================================================

    private void visitBlock(
            BlockNode node) {

        // Create a new scope.
        symbolTable.enterScope();


        for (StatementNode statement :
                node.getStatements()) {

            visitStatement(statement);
        }


        // Leave the scope.
        symbolTable.exitScope();
    }


    // =====================================================
    // EXPRESSION ANALYSIS
    // =====================================================

    private SemanticType visitExpression(
            ExpressionNode node) {


        // -------------------------------------------------
        // INTEGER LITERAL
        // -------------------------------------------------

        if (node instanceof IntegerLiteralNode) {

            return SemanticType.INTEGER;
        }


        // -------------------------------------------------
        // BOOLEAN LITERAL
        // -------------------------------------------------

        if (node instanceof BooleanLiteralNode) {

            return SemanticType.BOOLEAN;
        }


        // -------------------------------------------------
        // IDENTIFIER
        // -------------------------------------------------

        if (node instanceof IdentifierNode identifier) {

            Symbol symbol =
                    symbolTable.lookup(
                            identifier.getName()
                    );


            // Undeclared variable.
            if (symbol == null) {

                error(
                        identifier.getLine(),
                        "Variable '"
                                + identifier.getName()
                                + "' used before declaration."
                );

                return SemanticType.ERROR;
            }


            // Declared but not initialized.
            if (!symbol.isInitialized()) {

                error(
                        identifier.getLine(),
                        "Variable '"
                                + identifier.getName()
                                + "' used before initialization."
                );
            }


            if (symbol.getType()
                    == TokenType.INTEGER_TYPE) {

                return SemanticType.INTEGER;

            } else {

                return SemanticType.BOOLEAN;
            }
        }


        // -------------------------------------------------
        // UNARY EXPRESSION
        // -------------------------------------------------

        if (node instanceof UnaryExpressionNode unary) {

            SemanticType operand =
                    visitExpression(
                            unary.getOperand()
                    );


            if (operand == SemanticType.ERROR) {

                return SemanticType.ERROR;
            }


            TokenType op =
                    unary.getOperator().getType();


            // Unary minus
            if (op == TokenType.MINUS) {

                if (operand != SemanticType.INTEGER) {

                    error(
                            unary.getLine(),
                            "Unary '-' requires an Integer operand."
                    );

                    return SemanticType.ERROR;
                }

                return SemanticType.INTEGER;
            }


            // Logical NOT
            if (op == TokenType.NOT) {

                if (operand != SemanticType.BOOLEAN) {

                    error(
                            unary.getLine(),
                            "'না' requires a Boolean operand."
                    );

                    return SemanticType.ERROR;
                }

                return SemanticType.BOOLEAN;
            }


            error(
                    unary.getLine(),
                    "Unsupported unary operator."
            );

            return SemanticType.ERROR;
        }


        // -------------------------------------------------
        // BINARY EXPRESSION
        // -------------------------------------------------

        if (node instanceof BinaryExpressionNode binary) {

            SemanticType left =
                    visitExpression(
                            binary.getLeft()
                    );

            SemanticType right =
                    visitExpression(
                            binary.getRight()
                    );


            if (left == SemanticType.ERROR
                    || right == SemanticType.ERROR) {

                return SemanticType.ERROR;
            }


            TokenType op =
                    binary.getOperator().getType();


            switch (op) {

                // =========================================
                // ARITHMETIC
                // =========================================

                case PLUS,
                     MINUS,
                     MULTIPLY,
                     DIVIDE -> {

                    if (left != SemanticType.INTEGER
                            || right != SemanticType.INTEGER) {

                        error(
                                binary.getLine(),
                                "Arithmetic operator '"
                                        + binary.getOperator().getLexeme()
                                        + "' requires Integer operands."
                        );

                        return SemanticType.ERROR;
                    }

                    return SemanticType.INTEGER;
                }


                // =========================================
                // RELATIONAL
                // =========================================

                case LESS,
                     LESS_EQUAL,
                     GREATER,
                     GREATER_EQUAL -> {

                    if (left != SemanticType.INTEGER
                            || right != SemanticType.INTEGER) {

                        error(
                                binary.getLine(),
                                "Relational operator '"
                                        + binary.getOperator().getLexeme()
                                        + "' requires Integer operands."
                        );

                        return SemanticType.ERROR;
                    }

                    return SemanticType.BOOLEAN;
                }


                // =========================================
                // EQUALITY
                // =========================================

                case EQUAL_EQUAL,
                     NOT_EQUAL -> {

                    if (left != right) {

                        error(
                                binary.getLine(),
                                "Equality operator '"
                                        + binary.getOperator().getLexeme()
                                        + "' requires operands of the same type."
                        );

                        return SemanticType.ERROR;
                    }

                    return SemanticType.BOOLEAN;
                }


                // =========================================
                // LOGICAL AND / OR
                // =========================================

                case AND,
                     OR -> {

                    if (left != SemanticType.BOOLEAN
                            || right != SemanticType.BOOLEAN) {

                        error(
                                binary.getLine(),
                                "Logical operator '"
                                        + binary.getOperator().getLexeme()
                                        + "' requires Boolean operands."
                        );

                        return SemanticType.ERROR;
                    }

                    return SemanticType.BOOLEAN;
                }


                // =========================================
                // UNKNOWN OPERATOR
                // =========================================

                default -> {

                    error(
                            binary.getLine(),
                            "Unsupported binary operator '"
                                    + binary.getOperator().getLexeme()
                                    + "'."
                    );

                    return SemanticType.ERROR;
                }
            }
        }


        return SemanticType.ERROR;
    }


    // =====================================================
    // ERROR HANDLER
    // =====================================================

    private void error(
            int line,
            String message) {

        errors.add(
                "Semantic Error [Line "
                        + line
                        + "]: "
                        + message
        );
    }
}