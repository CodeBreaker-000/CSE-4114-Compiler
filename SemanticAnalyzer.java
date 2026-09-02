import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
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
    private final java.util.List<String> errors = new java.util.ArrayList<>();

    public SemanticAnalyzer() {
        beginScope();
    }

    public void analyze(Ast.Program program) {
        if (program == null) {
            error(1, "Cannot analyze a null program.");
            return;
        }
        for (Ast.Statement statement : program.statements) {
            analyzeStatement(statement);
        }
    }

    public java.util.List<String> getErrors() {
        return java.util.Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private void analyzeStatement(Ast.Statement statement) {
        if (statement instanceof Ast.Declaration) analyzeDeclaration((Ast.Declaration) statement);
        else if (statement instanceof Ast.Assignment) analyzeAssignment((Ast.Assignment) statement);
        else if (statement instanceof Ast.Print) analyzeExpression(((Ast.Print) statement).value);
        else if (statement instanceof Ast.If) analyzeIf((Ast.If) statement);
        else if (statement instanceof Ast.While) analyzeWhile((Ast.While) statement);
        else if (statement instanceof Ast.Block) analyzeBlockInNewScope((Ast.Block) statement);
    }

    private void analyzeDeclaration(Ast.Declaration d) {
        Type declaredType = fromTokenType(d.type);
        if (declaredType == Type.ERROR) {
            error(d.line, "Unknown declaration type for variable '" + d.name + "'.");
            return;
        }

        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(d.name)) {
            Symbol old = current.get(d.name);
            error(d.line, "Variable '" + d.name + "' is already declared in this scope (line "
                    + old.declarationLine + ").");
        } else {
            current.put(d.name, new Symbol(d.name, declaredType, d.line));
        }

        if (d.initializer != null) {
            Type actual = analyzeExpression(d.initializer);
            if (actual != Type.ERROR && actual != declaredType) {
                error(d.line, "Cannot initialize " + typeName(declaredType) + " variable '"
                        + d.name + "' with " + typeName(actual) + " expression.");
            }
        }
    }

    private void analyzeAssignment(Ast.Assignment a) {
        Symbol symbol = lookup(a.name);
        if (symbol == null) {
            error(a.line, "Variable '" + a.name + "' has not been declared.");
        }

        Type actual = analyzeExpression(a.value);
        if (symbol != null && actual != Type.ERROR && symbol.type != actual) {
            error(a.line, "Cannot assign " + typeName(actual) + " expression to "
                    + typeName(symbol.type) + " variable '" + a.name + "'.");
        }
    }

    private void analyzeIf(Ast.If s) {
        requireBoolean(analyzeExpression(s.condition), s.condition.line, "If condition");
        analyzeBlockInNewScope(s.thenBranch);

        for (Ast.ElseIf e : s.elseIfBranches) {
            requireBoolean(analyzeExpression(e.condition), e.condition.line, "Else-if condition");
            analyzeBlockInNewScope(e.body);
        }
        if (s.elseBranch != null) analyzeBlockInNewScope(s.elseBranch);
    }

    private void analyzeWhile(Ast.While s) {
        requireBoolean(analyzeExpression(s.condition), s.condition.line, "While condition");
        analyzeBlockInNewScope(s.body);
    }

    private void analyzeBlockInNewScope(Ast.Block block) {
        if (block == null) return;
        beginScope();
        for (Ast.Statement statement : block.statements) analyzeStatement(statement);
        endScope();
    }

    private Type analyzeExpression(Ast.Expression e) {
        if (e == null || e instanceof Ast.ErrorExpression) return Type.ERROR;
        if (e instanceof Ast.Literal) return typeOfLiteral((Ast.Literal) e);
        if (e instanceof Ast.Variable) {
            Ast.Variable v = (Ast.Variable) e;
            Symbol symbol = lookup(v.name);
            if (symbol == null) {
                error(v.line, "Variable '" + v.name + "' has not been declared.");
                return Type.ERROR;
            }
            return symbol.type;
        }
        if (e instanceof Ast.Unary) return analyzeUnary((Ast.Unary) e);
        if (e instanceof Ast.Binary) return analyzeBinary((Ast.Binary) e);
        return Type.ERROR;
    }

    private Type typeOfLiteral(Ast.Literal literal) {
        if (literal.type == TokenType.INTEGER_LITERAL) return Type.INT;
        if (literal.type == TokenType.TRUE || literal.type == TokenType.FALSE) return Type.BOOL;
        error(literal.line, "Unknown literal type.");
        return Type.ERROR;
    }

    private Type analyzeUnary(Ast.Unary u) {
        Type operand = analyzeExpression(u.operand);
        if (operand == Type.ERROR) return Type.ERROR;

        if (u.operator == TokenType.MINUS) {
            if (operand != Type.INT) {
                error(u.line, "Unary '-' requires an integer operand, but found " + typeName(operand) + ".");
                return Type.ERROR;
            }
            return Type.INT;
        }
        if (u.operator == TokenType.NOT) {
            if (operand != Type.BOOL) {
                error(u.line, "Unary '!' requires a boolean operand, but found " + typeName(operand) + ".");
                return Type.ERROR;
            }
            return Type.BOOL;
        }
        error(u.line, "Unsupported unary operator '" + u.operator + "'.");
        return Type.ERROR;
    }

    private Type analyzeBinary(Ast.Binary b) {
        Type left = analyzeExpression(b.left);
        Type right = analyzeExpression(b.right);
        if (left == Type.ERROR || right == Type.ERROR) return Type.ERROR;

        switch (b.operator) {
            case PLUS: case MINUS: case MULTIPLY: case DIVIDE: case MODULO:
                if (left != Type.INT || right != Type.INT) {
                    error(b.line, "Arithmetic operator '" + operatorName(b.operator)
                            + "' requires integer operands, but found " + typeName(left)
                            + " and " + typeName(right) + ".");
                    return Type.ERROR;
                }
                return Type.INT;
            case LESS: case LESS_EQUAL: case GREATER: case GREATER_EQUAL:
                if (left != Type.INT || right != Type.INT) {
                    error(b.line, "Relational operator '" + operatorName(b.operator)
                            + "' requires integer operands, but found " + typeName(left)
                            + " and " + typeName(right) + ".");
                    return Type.ERROR;
                }
                return Type.BOOL;
            case EQUAL_EQUAL: case NOT_EQUAL:
                if (left != right) {
                    error(b.line, "Equality operator '" + operatorName(b.operator)
                            + "' requires operands of the same type, but found "
                            + typeName(left) + " and " + typeName(right) + ".");
                    return Type.ERROR;
                }
                return Type.BOOL;
            default:
                error(b.line, "Unsupported binary operator '" + b.operator + "'.");
                return Type.ERROR;
        }
    }

    private void requireBoolean(Type type, int line, String context) {
        if (type != Type.ERROR && type != Type.BOOL) {
            error(line, context + " must be boolean, but found " + typeName(type) + ".");
        }
    }

    private Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name);
            if (symbol != null) return symbol;
        }
        return null;
    }

    private void beginScope() { scopes.push(new HashMap<String, Symbol>()); }
    private void endScope() { if (scopes.size() > 1) scopes.pop(); }

    private Type fromTokenType(TokenType type) {
        if (type == TokenType.INT_TYPE) return Type.INT;
        if (type == TokenType.BOOL_TYPE) return Type.BOOL;
        return Type.ERROR;
    }

    private String typeName(Type type) {
        switch (type) {
            case INT: return "INT";
            case BOOL: return "BOOL";
            default: return "ERROR";
        }
    }

    private String operatorName(TokenType op) {
        switch (op) {
            case PLUS: return "+";
            case MINUS: return "-";
            case MULTIPLY: return "*";
            case DIVIDE: return "/";
            case MODULO: return "%";
            case EQUAL_EQUAL: return "==";
            case NOT_EQUAL: return "!=";
            case LESS: return "<";
            case LESS_EQUAL: return "<=";
            case GREATER: return ">";
            case GREATER_EQUAL: return ">=";
            default: return op.toString();
        }
    }

    private void error(int line, String message) {
        errors.add("Semantic Error at line " + line + ": " + message);
    }
}
