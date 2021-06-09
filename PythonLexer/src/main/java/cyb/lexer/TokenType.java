package cyb.lexer;

public enum TokenType {
    IDENTIFIER(""),

    // KEYWORDS
    AND("and"),
    AS("as"),
    ASSERT("assert"),
    ASYNC("async"),
    AWAIT("await"),
    BREAK("break"),
    CLASS("class"),
    CONTINUE("continue"),
    DEF("def"),
    DEL("del"),
    ELIF("elif"),
    ELSE("else"),
    EXCEPT("except"),
    FALSE("False"),
    FINALLY("finally"),
    FOR("for"),
    FROM("from"),
    GLOBAL("global"),
    IF("if"),
    IMPORT("import"),
    IN("in"),
    IS("is"),
    LAMBDA("lambda"),
    NONE("None"),
    NONLOCAL("nonlocal"),
    NOT("not"),
    OR("or"),
    PASS("pass"),
    RAISE("raise"),
    RETURN("return"),
    TRUE("True"),
    TRY("try"),
    WHILE("while"),
    WITH("with"),
    YIELD("yield"),

    // OPERATORS
    PLUS("+"),
    MINUS("-"),
    ASTERISK("*"),
    POWER("**"),
    SLASH("/"),
    DOUBLE_SLASH("//"),
    PERCENT("%"),
    AT("@"),
    LEFT_SHIFT("<<"),
    RIGHT_SHIFT(">>"),
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_XOR("^"),
    BITWISE_NOT("~"),
    COLON_ASSIGN(":="),
    LESS("<"),
    GREATER(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    EQUAL("=="),
    NOT_EQUAL("!="),

    // DELIMITERS
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
    LEFT_SQUARE_BRACKET("["),
    RIGHT_SQUARE_BRACKET("]"),
    LEFT_CURLY_BRACKET("{"),
    RIGHT_CURLY_BRACKET("}"),
    COMMA(","),
    COLON(":"),
    DOT("."),
    SEMICOLON(";"),
    ASSIGN("="),
    ARROW("->"),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    ASTERISK_ASSIGN("*="),
    SLASH_ASSIGN("/="),
    DOUBLE_SLASH_ASSIGN("//="),
    PERCENT_ASSIGN("%="),
    AT_ASSIGN("@="),
    BITWISE_AND_ASSIGN("&="),
    BITWISE_OR_ASSIGN("|="),
    BITWISE_XOR_ASSIGN("^="),
    LEFT_SHIFT_ASSIGN("<<="),
    RIGHT_SHIFT_ASSIGN(">>="),
    POWER_ASSIGN("**="),

    // NUMERIC LITERALS
    INTEGER_LITERAL(""),
    FLOATING_POINT_LITERAL(""),
    IMAGINARY_LITERAL(""),

    STRING_LITERAL(""),

    // LINE STRUCTURE
    NEWLINE(""),
    INDENT(""),
    DEDENT(""),

    ERROR("");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
