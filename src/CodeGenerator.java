public class CodeGenerator {

    private final StringBuilder output = new StringBuilder();
    private int indentLevel = 0;

    public String generate(Ast.Program program) {
        if (program == null) {
            throw new IllegalArgumentException("Program cannot be null.");
        }

        output.setLength(0);
        indentLevel = 0;

        line("public class GeneratedProgram {");
        indentLevel++;

        line("public static void main(String[] args) {");
        indentLevel++;

        for (Ast.Statement statement : program.statements) {
            generateStatement(statement);
        }

        indentLevel--;
        line("}");

        indentLevel--;
        line("}");

        return output.toString();
    }

    private void generateStatement(Ast.Statement statement) {
        if (statement instanceof Ast.Declaration) {
            generateDeclaration((Ast.Declaration) statement);

        } else if (statement instanceof Ast.Assignment) {
            Ast.Assignment assignment = (Ast.Assignment) statement;
            line(assignment.name + " = " + generateExpression(assignment.value) + ";");

        } else if (statement instanceof Ast.Print) {
            Ast.Print print = (Ast.Print) statement;
            line("System.out.println(" + generateExpression(print.value) + ");");

        } else if (statement instanceof Ast.If) {
            generateIf((Ast.If) statement);

        } else if (statement instanceof Ast.While) {
            generateWhile((Ast.While) statement);

        } else if (statement instanceof Ast.Block) {
            generateStandaloneBlock((Ast.Block) statement);

        } else if (statement != null) {
            throw new IllegalStateException(
                    "Unsupported statement node: " + statement.getClass().getSimpleName()
            );
        }
    }

    private void generateDeclaration(Ast.Declaration declaration) {
        String javaType;
        String defaultValue;

        if (declaration.type == TokenType.INT_TYPE) {
            javaType = "int";
            defaultValue = "0";

        } else if (declaration.type == TokenType.BOOL_TYPE) {
            javaType = "boolean";
            defaultValue = "false";

        } else {
            throw new IllegalStateException(
                    "Unsupported declaration type: " + declaration.type
            );
        }

        String value;

        if (declaration.initializer == null) {
            value = defaultValue;
        } else {
            value = generateExpression(declaration.initializer);
        }

        line(javaType + " " + declaration.name + " = " + value + ";");
    }

    private void generateIf(Ast.If statement) {
        line("if (" + generateExpression(statement.condition) + ") {");

        generateBlockContents(statement.thenBranch);

        line("}");

        for (Ast.ElseIf elseIf : statement.elseIfBranches) {
            line("else if (" + generateExpression(elseIf.condition) + ") {");

            generateBlockContents(elseIf.body);

            line("}");
        }

        if (statement.elseBranch != null) {
            line("else {");

            generateBlockContents(statement.elseBranch);

            line("}");
        }
    }

    private void generateWhile(Ast.While statement) {
        line("while (" + generateExpression(statement.condition) + ") {");

        generateBlockContents(statement.body);

        line("}");
    }

    private void generateStandaloneBlock(Ast.Block block) {
        line("{");

        generateBlockContents(block);

        line("}");
    }

    private void generateBlockContents(Ast.Block block) {
        if (block == null) {
            return;
        }

        indentLevel++;

        for (Ast.Statement statement : block.statements) {
            generateStatement(statement);
        }

        indentLevel--;
    }

    private String generateExpression(Ast.Expression expression) {
        if (expression instanceof Ast.Literal) {
            Ast.Literal literal = (Ast.Literal) expression;

            if (literal.type == TokenType.TRUE) {
                return "true";
            }

            if (literal.type == TokenType.FALSE) {
                return "false";
            }

            if (literal.type == TokenType.INTEGER_LITERAL) {
                return literal.value;
            }

            throw new IllegalStateException(
                    "Unsupported literal type: " + literal.type
            );
        }

        if (expression instanceof Ast.Variable) {
            return ((Ast.Variable) expression).name;
        }

        if (expression instanceof Ast.Unary) {
            Ast.Unary unary = (Ast.Unary) expression;

            return "("
                    + operatorText(unary.operator)
                    + generateExpression(unary.operand)
                    + ")";
        }

        if (expression instanceof Ast.Binary) {
            Ast.Binary binary = (Ast.Binary) expression;

            return "("
                    + generateExpression(binary.left)
                    + " "
                    + operatorText(binary.operator)
                    + " "
                    + generateExpression(binary.right)
                    + ")";
        }

        if (expression instanceof Ast.ErrorExpression) {
            throw new IllegalStateException(
                    "Cannot generate code for an invalid expression."
            );
        }

        throw new IllegalStateException("Unsupported expression node.");
    }

    private String operatorText(TokenType operator) {
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

            case NOT:
                return "!";

            default:
                throw new IllegalStateException(
                        "Unsupported operator: " + operator
                );
        }
    }

    private void line(String text) {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }

        output.append(text).append(System.lineSeparator());
    }
}