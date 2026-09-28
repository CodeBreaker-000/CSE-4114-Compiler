import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Semantic analyzer for the Bangla language. */
public class SemanticAnalyzer {

    public enum Type { INT, BOOL, ERROR }

    private static final class Symbol {
        final String name;
        final Type type;
        final int declarationLine;

        Symbol(String name, Type type, int declarationLine) {
            this.name = name;
            this.type = type;
            this.declarationLine = declarationLine;
        }
    }

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
    private final List<String> errors = new ArrayList<>();

    public SemanticAnalyzer() {
        beginScope();
    }

    /**
     * Performs semantic analysis on one complete program.
     *
     * A SemanticAnalyzer instance can be reused safely: every call starts with
     * a fresh global scope and a fresh error list.
     */
    public void analyze(Ast.Program program) {
        scopes.clear();
        beginScope();
        errors.clear();

        if (program == null) {
            error(1, "Cannot analyze a null program.");
            return;
        }

        for (Ast.Statement statement : program.statements) {
            if (statement != null) {
                analyzeStatement(statement);
            }
        }
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private void analyzeStatement(Ast.Statement statement) {
        if (statement instanceof Ast.Declaration) {
            analyzeDeclaration((Ast.Declaration) statement);
        } else if (statement instanceof Ast.Assignment) {
            analyzeAssignment((Ast.Assignment) statement);
        } else if (statement instanceof Ast.Print) {
            analyzeExpression(((Ast.Print) statement).value);
        } else if (statement instanceof Ast.If) {
            analyzeIf((Ast.If) statement);
        } else if (statement instanceof Ast.While) {
            analyzeWhile((Ast.While) statement);
        } else if (statement instanceof Ast.Block) {
            analyzeBlockInNewScope((Ast.Block) statement);
        }
    }

    private void analyzeDeclaration(Ast.Declaration declaration) {
        Type declaredType = fromTokenType(declaration.type);

        if (declaredType == Type.ERROR) {
            error(declaration.line,
                    "Unknown declaration type for variable '" + declaration.name + "'.");
            return;
        }

        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(declaration.name)) {
            Symbol old = current.get(declaration.name);
            error(declaration.line,
                    "Variable '" + declaration.name
                            + "' is already declared in this scope (line "
                            + old.declarationLine + ").");
        } else {
            current.put(declaration.name,
                    new Symbol(declaration.name, declaredType, declaration.line));
        }

        if (declaration.initializer != null) {
            Type actual = analyzeExpression(declaration.initializer);
            if (actual != Type.ERROR && actual != declaredType) {
                error(declaration.line,
                        "Cannot initialize " + typeName(declaredType)
                                + " variable '" + declaration.name
                                + "' with " + typeName(actual) + " expression.");
            }
        }
    }

    private void analyzeAssignment(Ast.Assignment assignment) {
        Symbol symbol = lookup(assignment.name);
        if (symbol == null) {
            error(assignment.line,
                    "Variable '" + assignment.name + "' has not been declared.");
        }

        Type actual = analyzeExpression(assignment.value);
        if (symbol != null && actual != Type.ERROR && symbol.type != actual) {
            error(assignment.line,
                    "Cannot assign " + typeName(actual) + " expression to "
                            + typeName(symbol.type) + " variable '"
                            + assignment.name + "'.");
        }
    }

    private void analyzeIf(Ast.If statement) {
        requireBoolean(
                analyzeExpression(statement.condition),
                statement.condition.line,
                "If condition"
        );

        analyzeBlockInNewScope(statement.thenBranch);

        for (Ast.ElseIf elseIf : statement.elseIfBranches) {
            requireBoolean(
                    analyzeExpression(elseIf.condition),
                    elseIf.condition.line,
                    "Else-if condition"
            );
            analyzeBlockInNewScope(elseIf.body);
        }

        if (statement.elseBranch != null) {
            analyzeBlockInNewScope(statement.elseBranch);
        }
    }

    private void analyzeWhile(Ast.While statement) {
        requireBoolean(
                analyzeExpression(statement.condition),
                statement.condition.line,
                "While condition"
        );
        analyzeBlockInNewScope(statement.body);
    }

    private void analyzeBlockInNewScope(Ast.Block block) {
        if (block == null) {
            return;
        }

        beginScope();
        for (Ast.Statement statement : block.statements) {
            if (statement != null) {
                analyzeStatement(statement);
            }
        }
        endScope();
    }

    private Type analyzeExpression(Ast.Expression expression) {
        if (expression == null || expression instanceof Ast.ErrorExpression) {
            return Type.ERROR;
        }

        if (expression instanceof Ast.Literal) {
            return typeOfLiteral((Ast.Literal) expression);
        }

        if (expression instanceof Ast.Variable) {
            Ast.Variable variable = (Ast.Variable) expression;
            Symbol symbol = lookup(variable.name);

            if (symbol == null) {
                error(variable.line,
                        "Variable '" + variable.name + "' has not been declared.");
                return Type.ERROR;
            }

            return symbol.type;
        }

        if (expression instanceof Ast.Unary) {
            return analyzeUnary((Ast.Unary) expression);
        }

        if (expression instanceof Ast.Binary) {
            return analyzeBinary((Ast.Binary) expression);
        }

        return Type.ERROR;
    }

    private Type typeOfLiteral(Ast.Literal literal) {
        if (literal.type == TokenType.INTEGER_LITERAL) {
            return Type.INT;
        }

        if (literal.type == TokenType.TRUE || literal.type == TokenType.FALSE) {
            return Type.BOOL;
        }

        error(literal.line, "Unknown literal type.");
        return Type.ERROR;
    }

    private Type analyzeUnary(Ast.Unary unary) {
        Type operand = analyzeExpression(unary.operand);

        if (operand == Type.ERROR) {
            return Type.ERROR;
        }

        if (unary.operator == TokenType.MINUS) {
            if (operand != Type.INT) {
                error(
                        unary.line,
                        "Unary '-' requires an integer operand, but found "
                                + typeName(operand) + "."
                );
                return Type.ERROR;
            }
            return Type.INT;
        }

        if (unary.operator == TokenType.NOT) {
            if (operand != Type.BOOL) {
                error(
                        unary.line,
                        "Unary '!' requires a boolean operand, but found "
                                + typeName(operand) + "."
                );
                return Type.ERROR;
            }
            return Type.BOOL;
        }

        error(unary.line,
                "Unsupported unary operator '" + unary.operator + "'.");
        return Type.ERROR;
    }

    private Type analyzeBinary(Ast.Binary binary) {
        Type left = analyzeExpression(binary.left);
        Type right = analyzeExpression(binary.right);

        if (left == Type.ERROR || right == Type.ERROR) {
            return Type.ERROR;
        }

        switch (binary.operator) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                if (left != Type.INT || right != Type.INT) {
                    error(
                            binary.line,
                            "Arithmetic operator '" + operatorName(binary.operator)
                                    + "' requires integer operands, but found "
                                    + typeName(left) + " and " + typeName(right) + "."
                    );
                    return Type.ERROR;
                }
                return Type.INT;

            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
                if (left != Type.INT || right != Type.INT) {
                    error(
                            binary.line,
                            "Relational operator '" + operatorName(binary.operator)
                                    + "' requires integer operands, but found "
                                    + typeName(left) + " and " + typeName(right) + "."
                    );
                    return Type.ERROR;
                }
                return Type.BOOL;

            case EQUAL_EQUAL:
            case NOT_EQUAL:
                if (left != right) {
                    error(
                            binary.line,
                            "Equality operator '" + operatorName(binary.operator)
                                    + "' requires operands of the same type, but found "
                                    + typeName(left) + " and " + typeName(right) + "."
                    );
                    return Type.ERROR;
                }
                return Type.BOOL;

            default:
                error(
                        binary.line,
                        "Unsupported binary operator '" + binary.operator + "'."
                );
                return Type.ERROR;
        }
    }

    private void requireBoolean(Type type, int line, String context) {
        if (type != Type.ERROR && type != Type.BOOL) {
            error(
                    line,
                    context + " must be boolean, but found " + typeName(type) + "."
            );
        }
    }

    private Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Symbol>());
    }

    private void endScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    private Type fromTokenType(TokenType type) {
        if (type == TokenType.INT_TYPE) {
            return Type.INT;
        }

        if (type == TokenType.BOOL_TYPE) {
            return Type.BOOL;
        }

        return Type.ERROR;
    }

    private String typeName(Type type) {
        switch (type) {
            case INT:
                return "INT";
            case BOOL:
                return "BOOL";
            default:
                return "ERROR";
        }
    }

    private String operatorName(TokenType operator) {
        switch (operator) {
            case PLUS:
                return "+";
            case MINUS:
                return "-";
            case MULTIPLY:
                return "*";
            case DIVIDE:
                return "/";
            case MODULO:
                return "%";
            case EQUAL_EQUAL:
                return "==";
            case NOT_EQUAL:
                return "!=";
            case LESS:
                return "<";
            case LESS_EQUAL:
                return "<=";
            case GREATER:
                return ">";
            case GREATER_EQUAL:
                return ">=";
            default:
                return operator.toString();
        }
    }

    private void error(int line, String message) {
        errors.add("Semantic Error at line " + line + ": " + message);
    }
}
