import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        try {

            String code = loadSource(args);


            Lexer lexer = new Lexer(code);

            List<Token> tokens =
                    lexer.tokenize();

            if (!lexer.getErrors().isEmpty()) {

                printErrors(
                        "LEXER ERRORS",
                        lexer.getErrors()
                );

                return;
            }



            Parser parser =
                    new Parser(tokens);

            Ast.Program program =
                    parser.parse();

            if (!parser.getErrors().isEmpty()) {

                printErrors(
                        "PARSER ERRORS",
                        parser.getErrors()
                );

                return;
            }

            SemanticAnalyzer semanticAnalyzer =
                    new SemanticAnalyzer();

            semanticAnalyzer.analyze(program);

            if (semanticAnalyzer.hasErrors()) {

                printErrors(
                        "SEMANTIC ERRORS",
                        semanticAnalyzer.getErrors()
                );

                return;
            }

            CodeGenerator generator =
                    new CodeGenerator();

            String javaCode =
                    generator.generate(program);



            Path outputFile =
                    Path.of(
                            "GeneratedProgram.java"
                    );

            Files.writeString(
                    outputFile,
                    javaCode,
                    StandardCharsets.UTF_8
            );


            System.out.println(
                    "Compilation successful."
            );

            System.out.println(
                    "Generated target file: "
                            + outputFile
                            .toAbsolutePath()
            );

        } catch (IOException exception) {

            System.err.println(
                    "File Error: "
                            + exception.getMessage()
            );

        } catch (RuntimeException exception) {

            System.err.println(
                    "Compiler Error: "
                            + exception.getMessage()
            );
        }
    }


    private static String loadSource(
            String[] args
    ) throws IOException {


        if (args.length > 0) {

            return Files.readString(
                    Path.of(args[0]),
                    StandardCharsets.UTF_8
            );
        }


        return """

                # Sample Bangla program

                পূর্ণ x <- 10;
                পূর্ণ y <- 5;

                যুক্তি বড় <- x > y;

                শর্ত (বড়) {
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
    }


    private static void printErrors(
            String title,
            List<String> errors
    ) {

        System.out.println(
                "----- "
                        + title
                        + " -----"
        );

        for (String error : errors) {

            System.out.println(error);
        }
    }
}