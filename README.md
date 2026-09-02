# CSE-4114 Compiler Project

A Bangla-based programming language compiler developed in Java for CSE-4114 Compiler Design and Construction Sessional.

## Parser

The recursive-descent parser builds an abstract syntax tree (AST) for integer and boolean declarations, assignments, arithmetic and comparison expressions, `প্রকাশ` statements, `শর্ত` / `অন্যশর্ত` / `অন্যথা` conditionals, and `চলবে` loops. Arithmetic precedence is supported (`*`, `/`, `%` before `+`, `-`), and syntax errors recover at a semicolon or closing brace.

Compile and run the sample program:

```bash
cd src
javac *.java
java Main
```

Run the parser regression tests:

```bash
cd src
javac *.java
java ParserTest
```
