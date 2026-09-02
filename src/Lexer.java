import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String code;
    private int pos;
    private int line;

    private final List<Token> tokens;
    private final List<String> errors;

    public Lexer(String code) {
        this.code = code;
        this.pos = 0;
        this.line = 1;
        this.tokens = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    private char currentChar() {
        if (pos >= code.length()) {
            return '\0';
        }

        return code.charAt(pos);
    }

    private char peekChar() {
        if (pos + 1 >= code.length()) {
            return '\0';
        }

        return code.charAt(pos + 1);
    }

    private void advance() {
        if (currentChar() == '\n') {
            line++;
        }

        pos++;
    }

    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, line));
    }

    private boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private boolean isIdentifierPart(char ch) {

        int type = Character.getType(ch);

        return Character.isLetterOrDigit(ch)
                || ch == '_'
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    public List<Token> tokenize() {

        while (currentChar() != '\0') {

            char ch = currentChar();

            if (Character.isWhitespace(ch)) {
                advance();
            }

            else if (ch == '#') {
                skipComment();
            }

            else if (Character.isDigit(ch)) {
                readNumber();
            }

            else if (isIdentifierStart(ch)) {
                readWord();
            }

            else if (ch == '<') {

                if (peekChar() == '-') {

                    addToken(TokenType.ASSIGN, "<-");

                    advance();
                    advance();
                }

                else if (peekChar() == '=') {

                    addToken(TokenType.LESS_EQUAL, "<=");

                    advance();
                    advance();
                }

                else {

                    addToken(TokenType.LESS, "<");

                    advance();
                }
            }

            else if (ch == '>') {

                if (peekChar() == '=') {

                    addToken(TokenType.GREATER_EQUAL, ">=");

                    advance();
                    advance();
                }

                else {

                    addToken(TokenType.GREATER, ">");

                    advance();
                }
            }

            else if (ch == '=') {

                if (peekChar() == '=') {

                    addToken(TokenType.EQUAL_EQUAL, "==");

                    advance();
                    advance();
                }

                else {

                    lexicalError(
                            "Unexpected '='. Use '<-' for assignment or '==' for equality."
                    );

                    advance();
                }
            }

            else if (ch == '!') {

                if (peekChar() == '=') {

                    addToken(TokenType.NOT_EQUAL, "!=");

                    advance();
                    advance();
                }

                else {

                    addToken(TokenType.NOT, "!");

                    advance();
                }
            }

            else if (ch == '+') {

                addToken(TokenType.PLUS, "+");

                advance();
            }

            else if (ch == '-') {

                addToken(TokenType.MINUS, "-");

                advance();
            }

            else if (ch == '*') {

                addToken(TokenType.MULTIPLY, "*");

                advance();
            }

            else if (ch == '/') {

                addToken(TokenType.DIVIDE, "/");

                advance();
            }

            else if (ch == '%') {

                addToken(TokenType.MODULO, "%");

                advance();
            }

            else if (ch == '(') {

                addToken(TokenType.LEFT_PAREN, "(");

                advance();
            }

            else if (ch == ')') {

                addToken(TokenType.RIGHT_PAREN, ")");

                advance();
            }

            else if (ch == '{') {

                addToken(TokenType.LEFT_BRACE, "{");

                advance();
            }

            else if (ch == '}') {

                addToken(TokenType.RIGHT_BRACE, "}");

                advance();
            }

            else if (ch == ';') {

                addToken(TokenType.SEMICOLON, ";");

                advance();
            }

            else {

                lexicalError(
                        "Illegal character '" + ch + "'"
                );

                advance();
            }
        }

        tokens.add(
                new Token(TokenType.EOF, null, line)
        );

        return tokens;
    }

    private void readNumber() {

        StringBuilder number = new StringBuilder();

        while (
                currentChar() != '\0'
                        &&
                Character.isDigit(currentChar())
        ) {

            number.append(currentChar());

            advance();
        }

        addToken(
                TokenType.INTEGER_LITERAL,
                number.toString()
        );
    }

    private void readWord() {

        StringBuilder word = new StringBuilder();

        while (
                currentChar() != '\0'
                        &&
                isIdentifierPart(currentChar())
        ) {

            word.append(currentChar());

            advance();
        }

        String value = word.toString();

        switch (value) {

            case "পূর্ণ":
                addToken(TokenType.INT_TYPE, value);
                break;

            case "যুক্তি":
                addToken(TokenType.BOOL_TYPE, value);
                break;

            case "সত্য":
                addToken(TokenType.TRUE, value);
                break;

            case "মিথ্যা":
                addToken(TokenType.FALSE, value);
                break;

            case "প্রকাশ":
                addToken(TokenType.PRINT, value);
                break;

            case "শর্ত":
                addToken(TokenType.IF, value);
                break;

            case "অন্যশর্ত":
                addToken(TokenType.ELSE_IF, value);
                break;

            case "অন্যথা":
                addToken(TokenType.ELSE, value);
                break;

            case "চলবে":
                addToken(TokenType.WHILE, value);
                break;

            default:
                addToken(
                        TokenType.IDENTIFIER,
                        value
                );
                break;
        }
    }

    private void skipComment() {

        while (
                currentChar() != '\0'
                        &&
                currentChar() != '\n'
        ) {

            advance();
        }
    }

    private void lexicalError(String message) {

        errors.add(
                "Lexer Error at line "
                        + line
                        + ": "
                        + message
        );
    }

    public List<String> getErrors() {
        return errors;
    }
}