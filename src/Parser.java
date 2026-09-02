import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for the Bangla language tokens emitted by Lexer.
 * It builds an AST and records syntax errors instead of aborting at the first
 * malformed statement.
 */
public class Parser {

    private final List<Token> tokens;
    private final List<String> errors = new ArrayList<>();
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens == null ? new ArrayList<Token>() : tokens;
    }

    public Ast.Program parse() {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            int before = current;
            Ast.Statement statement = statement();
            if (statement != null) {
                statements.add(statement);
            }
            if (current == before) {
                report(peek(), "Parser could not make progress.");
                advance();
            }
        }
        return new Ast.Program(statements);
    }

    public List<String> getErrors() {
        return errors;
    }

    private Ast.Statement statement() {
        if (match(TokenType.INT_TYPE, TokenType.BOOL_TYPE)) return declaration(previous());
        if (match(TokenType.IDENTIFIER)) return assignment(previous());
        if (match(TokenType.PRINT)) return print(previous());
        if (match(TokenType.IF)) return ifStatement(previous());
        if (match(TokenType.WHILE)) return whileStatement(previous());
        if (match(TokenType.LEFT_BRACE)) return block(previous());

        Token token = peek();
        report(token, "Expected a statement.");
        synchronize();
        return null;
    }

    private Ast.Statement declaration(Token type) {
        Token name = consume(TokenType.IDENTIFIER, "Expected an identifier after the type.");
        Ast.Expression initializer = null;
        if (match(TokenType.ASSIGN)) initializer = expression();
        consumeStatementEnd("Expected ';' after declaration.");
        return new Ast.Declaration(type.getLine(), type.getType(), name.getValue(), initializer);
    }

    private Ast.Statement assignment(Token name) {
        if (!match(TokenType.ASSIGN)) {
            report(peek(), "Expected '<-' after identifier.");
            synchronize();
            return null;
        }
        Ast.Expression value = expression();
        consumeStatementEnd("Expected ';' after assignment.");
        return new Ast.Assignment(name.getLine(), name.getValue(), value);
    }

    private Ast.Statement print(Token keyword) {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'প্রকাশ'.");
        Ast.Expression value = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after print expression.");
        consumeStatementEnd("Expected ';' after print statement.");
        return new Ast.Print(keyword.getLine(), value);
    }

    private Ast.Statement ifStatement(Token keyword) {
        Ast.Expression condition = parenthesizedCondition("'শর্ত'");
        Ast.Block thenBranch = requiredBlock("Expected '{' after if condition.");
        List<Ast.ElseIf> elseIfs = new ArrayList<>();
        while (match(TokenType.ELSE_IF)) {
            Token elseIf = previous();
            Ast.Expression elseIfCondition = parenthesizedCondition("'অন্যশর্ত'");
            elseIfs.add(new Ast.ElseIf(elseIf.getLine(), elseIfCondition,
                    requiredBlock("Expected '{' after else-if condition.")));
        }
        Ast.Block elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = requiredBlock("Expected '{' after 'অন্যথা'.");
        }
        return new Ast.If(keyword.getLine(), condition, thenBranch, elseIfs, elseBranch);
    }

    private Ast.Statement whileStatement(Token keyword) {
        Ast.Expression condition = parenthesizedCondition("'চলবে'");
        return new Ast.While(keyword.getLine(), condition,
                requiredBlock("Expected '{' after while condition."));
    }

    private Ast.Expression parenthesizedCondition(String keyword) {
        consume(TokenType.LEFT_PAREN, "Expected '(' after " + keyword + ".");
        Ast.Expression condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.");
        return condition;
    }

    private Ast.Block requiredBlock(String message) {
        Token brace = consume(TokenType.LEFT_BRACE, message);
        return block(brace);
    }

    private Ast.Block block(Token openingBrace) {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            int before = current;
            Ast.Statement statement = statement();
            if (statement != null) statements.add(statement);
            if (current == before) advance();
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
        return new Ast.Block(openingBrace.getLine(), statements);
    }

    // Lowest to highest precedence: comparison, addition, multiplication, unary, primary.
    private Ast.Expression expression() { return comparison(); }

    private Ast.Expression comparison() {
        Ast.Expression expression = addition();
        while (match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL, TokenType.LESS,
                TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            expression = new Ast.Binary(expression, operator.getType(), addition());
        }
        return expression;
    }

    private Ast.Expression addition() {
        Ast.Expression expression = multiplication();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            expression = new Ast.Binary(expression, operator.getType(), multiplication());
        }
        return expression;
    }

    private Ast.Expression multiplication() {
        Ast.Expression expression = unary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            Token operator = previous();
            expression = new Ast.Binary(expression, operator.getType(), unary());
        }
        return expression;
    }

    private Ast.Expression unary() {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            Token operator = previous();
            return new Ast.Unary(operator.getLine(), operator.getType(), unary());
        }
        return primary();
    }

    private Ast.Expression primary() {
        if (match(TokenType.INTEGER_LITERAL, TokenType.TRUE, TokenType.FALSE)) {
            Token token = previous();
            return new Ast.Literal(token.getLine(), token.getValue(), token.getType());
        }
        if (match(TokenType.IDENTIFIER)) {
            Token token = previous();
            return new Ast.Variable(token.getLine(), token.getValue());
        }
        if (match(TokenType.LEFT_PAREN)) {
            Ast.Expression expression = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return expression;
        }
        Token token = peek();
        report(token, "Expected an expression.");
        if (!isAtEnd()) advance();
        return new Ast.ErrorExpression(token.getLine());
    }

    private void consumeStatementEnd(String message) {
        if (!match(TokenType.SEMICOLON)) {
            report(peek(), message);
            synchronize();
        }
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        report(peek(), message);
        return new Token(type, "<missing>", peek().getLine());
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return current >= tokens.size() || peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        if (tokens.isEmpty()) return new Token(TokenType.EOF, null, 1);
        return tokens.get(Math.min(current, tokens.size() - 1));
    }

    private Token previous() {
        return tokens.get(Math.max(0, current - 1));
    }

    private void report(Token token, String message) {
        errors.add("Parser Error at line " + token.getLine() + ": " + message);
    }

    /** Recovers at a statement boundary, including a closing brace. */
    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON || check(TokenType.RIGHT_BRACE)) return;
            if (check(TokenType.INT_TYPE) || check(TokenType.BOOL_TYPE) || check(TokenType.IDENTIFIER)
                    || check(TokenType.PRINT) || check(TokenType.IF) || check(TokenType.WHILE)
                    || check(TokenType.LEFT_BRACE)) return;
            advance();
        }
    }
}
