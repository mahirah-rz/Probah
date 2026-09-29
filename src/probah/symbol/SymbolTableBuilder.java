package probah.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import probah.ast.AssignmentNode;
import probah.ast.BlockNode;
import probah.ast.BooleanLiteralNode;
import probah.ast.DeclarationNode;
import probah.ast.ExpressionNode;
import probah.ast.IfNode;
import probah.ast.IntegerLiteralNode;
import probah.ast.PrintNode;
import probah.ast.ProgramNode;
import probah.ast.StatementNode;
import probah.ast.UnaryExpressionNode;
import probah.ast.BinaryExpressionNode;
import probah.ast.IdentifierNode;
import probah.ast.WhileNode;


public class SymbolTableBuilder {

    private final SymbolTable symbolTable =
            new SymbolTable();

    private final List<String> errors =
            new ArrayList<>();


    public SymbolTable build(
            ProgramNode program
    ) {

        for (StatementNode statement :
                program.getStatements()) {

            visitStatement(statement);
        }

        return symbolTable;
    }


    public List<String> getErrors() {

        return Collections.unmodifiableList(
                errors
        );
    }


    public SymbolTable getSymbolTable() {

        return symbolTable;
    }


    private void visitStatement(
            StatementNode statement
    ) {

        if (statement instanceof DeclarationNode declaration) {

            visitDeclaration(declaration);

        } else if (statement instanceof AssignmentNode assignment) {

            visitAssignment(assignment);

        } else if (statement instanceof PrintNode print) {

            visitExpression(
                    print.getExpression()
            );

        } else if (statement instanceof IfNode ifNode) {

            visitIf(ifNode);

        } else if (statement instanceof WhileNode whileNode) {

            visitWhile(whileNode);

        } else if (statement instanceof BlockNode block) {

            visitBlock(block);
        }
    }


    private void visitDeclaration(
            DeclarationNode declaration
    ) {

        boolean inserted =
                symbolTable.define(
                        declaration.getName(),
                        declaration.getDeclaredType(),
                        declaration.getLine()
                );


        if (!inserted) {

            errors.add(
                    String.format(
                            "Symbol Table Error "
                            + "[Line %d]: "
                            + "Variable '%s' is already "
                            + "declared in scope '%s'.",

                            declaration.getLine(),
                            declaration.getName(),
                            symbolTable.currentScopeName()
                    )
            );

            
            return;
        }


        
        visitExpression(
                declaration.getInitializer()
        );
    }


    private void visitAssignment(
            AssignmentNode assignment
    ) {

       
        visitExpression(
                assignment.getValue()
        );
    }


    private void visitIf(
            IfNode ifNode
    ) {

        
        visitExpression(
                ifNode.getCondition()
        );


        
        visitBlock(
                ifNode.getThenBranch()
        );


        
        if (ifNode.getElseBranch() != null) {

            if (
                    ifNode.getElseBranch()
                    instanceof BlockNode block
            ) {

                visitBlock(block);

            } else {

                visitStatement(
                        ifNode.getElseBranch()
                );
            }
        }
    }


    private void visitWhile(
            WhileNode whileNode
    ) {

        visitExpression(
                whileNode.getCondition()
        );


        visitBlock(
                whileNode.getBody()
        );
    }


    private void visitBlock(
            BlockNode block
    ) {

        symbolTable.enterScope();


        for (StatementNode statement :
                block.getStatements()) {

            visitStatement(statement);
        }


        symbolTable.exitScope();
    }


    private void visitExpression(
            ExpressionNode expression
    ) {

        if (expression == null) {
            return;
        }


        if (
                expression
                instanceof IdentifierNode
        ) {

            
            symbolTable.lookup(
                    ((IdentifierNode) expression)
                            .getName()
            );

        } else if (
                expression
                instanceof BinaryExpressionNode binary
        ) {

            visitExpression(
                    binary.getLeft()
            );

            visitExpression(
                    binary.getRight()
            );

        } else if (
                expression
                instanceof UnaryExpressionNode unary
        ) {

            visitExpression(
                    unary.getOperand()
            );

        } else if (
                expression
                instanceof IntegerLiteralNode
        ) {

           

        } else if (
                expression
                instanceof BooleanLiteralNode
        ) {

            
        }
    }
}