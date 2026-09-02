import java.util.Collections;
import java.util.List;

/** Immutable abstract syntax tree nodes produced by {@link Parser}. */
public final class Ast {

    private Ast() {
    }

    public static final class Program {
        public final List<Statement> statements;

        public Program(List<Statement> statements) {
            this.statements = Collections.unmodifiableList(statements);
        }
    }

    public abstract static class Node {
        public final int line;

        protected Node(int line) {
            this.line = line;
        }
    }

    public abstract static class Statement extends Node {
        protected Statement(int line) {
            super(line);
        }
    }

    public static final class Block extends Statement {
        public final List<Statement> statements;

        public Block(int line, List<Statement> statements) {
            super(line);
            this.statements = Collections.unmodifiableList(statements);
        }
    }

    public static final class Declaration extends Statement {
        public final TokenType type;
        public final String name;
        public final Expression initializer;

        public Declaration(int line, TokenType type, String name, Expression initializer) {
            super(line);
            this.type = type;
            this.name = name;
            this.initializer = initializer;
        }
    }

    public static final class Assignment extends Statement {
        public final String name;
        public final Expression value;

        public Assignment(int line, String name, Expression value) {
            super(line);
            this.name = name;
            this.value = value;
        }
    }

    public static final class Print extends Statement {
        public final Expression value;

        public Print(int line, Expression value) {
            super(line);
            this.value = value;
        }
    }

    public static final class If extends Statement {
        public final Expression condition;
        public final Block thenBranch;
        public final List<ElseIf> elseIfBranches;
        public final Block elseBranch;

        public If(int line, Expression condition, Block thenBranch,
                  List<ElseIf> elseIfBranches, Block elseBranch) {
            super(line);
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseIfBranches = Collections.unmodifiableList(elseIfBranches);
            this.elseBranch = elseBranch;
        }
    }

    public static final class ElseIf extends Node {
        public final Expression condition;
        public final Block body;

        public ElseIf(int line, Expression condition, Block body) {
            super(line);
            this.condition = condition;
            this.body = body;
        }
    }

    public static final class While extends Statement {
        public final Expression condition;
        public final Block body;

        public While(int line, Expression condition, Block body) {
            super(line);
            this.condition = condition;
            this.body = body;
        }
    }

    public abstract static class Expression extends Node {
        protected Expression(int line) {
            super(line);
        }
    }

    public static final class Binary extends Expression {
        public final Expression left;
        public final TokenType operator;
        public final Expression right;

        public Binary(Expression left, TokenType operator, Expression right) {
            super(left.line);
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }

    public static final class Unary extends Expression {
        public final TokenType operator;
        public final Expression operand;

        public Unary(int line, TokenType operator, Expression operand) {
            super(line);
            this.operator = operator;
            this.operand = operand;
        }
    }

    public static final class Literal extends Expression {
        public final String value;
        public final TokenType type;

        public Literal(int line, String value, TokenType type) {
            super(line);
            this.value = value;
            this.type = type;
        }
    }

    public static final class Variable extends Expression {
        public final String name;

        public Variable(int line, String name) {
            super(line);
            this.name = name;
        }
    }

    /** Allows parsing to continue after a malformed expression. */
    public static final class ErrorExpression extends Expression {
        public ErrorExpression(int line) {
            super(line);
        }
    }
}
