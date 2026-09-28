import java.util.List;

public class Main {

    public static void main(String[] args) {

        String code = """

                # Sample Bangla program

                পূর্ণ x <- 10;
                পূর্ণ y <- 5;

                যুক্তি বড় <- x > y;

                শর্ত (x > y) {
                    প্রকাশ(x);
                }
                অন্যশর্ত (x == y) {
                    প্রকাশ(0);
                }
                অন্যথা {
                    প্রকাশ(y);
                }

                চলবে (x < 15) {
                    x <- x + 1;
                }

                প্রকাশ(x);

                """;


        Lexer lexer = new Lexer(code);


        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        Ast.Program program = parser.parse();

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(program);


        System.out.println("----- TOKENS -----");


        for (Token token : tokens) {

            System.out.println(token);
        }


        if (!lexer.getErrors().isEmpty()) {

            System.out.println(
                    "\n----- LEXER ERRORS -----"
            );


            for (String error : lexer.getErrors()) {

                System.out.println(error);
            }
        }

        System.out.println("\n----- PARSER -----");
        System.out.println("Parsed " + program.statements.size() + " top-level statement(s).");

        if (!parser.getErrors().isEmpty()) {
            System.out.println("\n----- PARSER ERRORS -----");
            for (String error : parser.getErrors()) {
                System.out.println(error);
            }
        }

        System.out.println("\n----- SEMANTIC ANALYSIS -----");

        if (semanticAnalyzer.hasErrors()) {
            for (String error : semanticAnalyzer.getErrors()) {
                System.out.println(error);
            }
        } else {
            System.out.println("Semantic analysis passed. No semantic errors.");
        }
    }
}
