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
    }
}