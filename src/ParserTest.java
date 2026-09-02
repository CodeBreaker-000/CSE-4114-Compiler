import java.util.List;

/** Small dependency-free parser regression tests. Run with: java ParserTest */
public class ParserTest {
    public static void main(String[] args) {
        parsesCompleteProgram();
        honorsArithmeticPrecedence();
        recoversAfterMissingSemicolon();
        System.out.println("Parser tests passed.");
    }

    private static void parsesCompleteProgram() {
        Ast.Program program = parse("""
                পূর্ণ x <- 10;
                যুক্তি বড় <- x > 5;
                শর্ত (বড়) { প্রকাশ(x); } অন্যশর্ত (x == 5) { প্রকাশ(5); } অন্যথা { প্রকাশ(0); }
                চলবে (x < 12) { x <- x + 1; }
                """);
        require(program.statements.size() == 4, "expected four top-level statements");
        require(program.statements.get(2) instanceof Ast.If, "expected an if statement");
        require(program.statements.get(3) instanceof Ast.While, "expected a while statement");
    }

    private static void honorsArithmeticPrecedence() {
        Ast.Program program = parse("পূর্ণ x <- 1 + 2 * 3;");
        Ast.Declaration declaration = (Ast.Declaration) program.statements.get(0);
        Ast.Binary sum = (Ast.Binary) declaration.initializer;
        require(sum.operator == TokenType.PLUS, "addition should be the outer operation");
        require(((Ast.Binary) sum.right).operator == TokenType.MULTIPLY,
                "multiplication should bind more tightly than addition");
    }

    private static void recoversAfterMissingSemicolon() {
        Lexer lexer = new Lexer("পূর্ণ x <- 1 প্রকাশ(x);");
        Parser parser = new Parser(lexer.tokenize());
        Ast.Program program = parser.parse();
        require(!parser.getErrors().isEmpty(), "missing semicolon should be reported");
        require(program.statements.size() == 2, "parser should continue at the next statement");
    }

    private static Ast.Program parse(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        require(lexer.getErrors().isEmpty(), "test source must lex without errors");
        Parser parser = new Parser(tokens);
        Ast.Program program = parser.parse();
        require(parser.getErrors().isEmpty(), "valid program must parse without errors: " + parser.getErrors());
        return program;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
