package cyb.lexer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

enum StringType {
    NONE,
    SINGLE_QUOTED,
    DOUBLE_QUOTED,
    TRIPLE_QUOTED
}

public class Lexer {
    private static final int TAB_STOP_LENGTH = 8;
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        boolean keywordsStarted = false;

        for (TokenType type : TokenType.values()) {
            if (keywordsStarted) {
                KEYWORDS.put(type.getValue(), type);
                if (type == TokenType.YIELD) {
                    break;
                }
            } else if (type == TokenType.AND) {
                keywordsStarted = true;
                KEYWORDS.put(type.getValue(), type);
            }
        }
    }

    private final InputStream in;
    private AutomatonState state = AutomatonState.INITIAL;
    private final List<Token> tokens = new ArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private int currentLine = 0;
    private int currentColumn = -1;
    private int tokenStartRow = 0;
    private int tokenStartColumn = 0;
    private char currentChar;

    private boolean blankLine = true;
    private final LinkedList<Integer> indentsStack = new LinkedList<>();
    private int currentIndent = 0;
    private StringType currentStringType = StringType.NONE;

    public Lexer(InputStream in) {
        this.in = in;
        indentsStack.add(0);
    }

    public List<Token> analyze() throws IOException {
        int result;

        while (true) {
            result = in.read();

            if (result < 0) {
                if (currentChar == '\n') {
                    return tokens;
                } else {
                    if (currentStringType == StringType.TRIPLE_QUOTED) {
                        endToken(TokenType.ERROR, "Missing closing triple quote.");
                        state = AutomatonState.INITIAL;
                    }
                    currentChar = '\n';
                }
            } else {
                currentChar = (char) result;
            }
            currentColumn++;

            switch (state) {
                case INITIAL -> setStateByCurrentChar();
                case KEYWORD_OR_IDENTIFIER -> parseKeywordOrIdentifier();
                case PLUS -> parseOperatorWithAlternative(TokenType.PLUS,
                        '=', AutomatonState.PLUS_ASSIGN);
                case PLUS_ASSIGN -> endToken(TokenType.PLUS_ASSIGN, TokenType.PLUS_ASSIGN.getValue());
                case MINUS -> parseOperatorWithDoubleAlternative(TokenType.MINUS,
                        '=', AutomatonState.MINUS_ASSIGN,
                        '>', AutomatonState.ARROW);
                case MINUS_ASSIGN -> endToken(TokenType.MINUS_ASSIGN, TokenType.MINUS_ASSIGN.getValue());
                case ARROW -> endToken(TokenType.ARROW, TokenType.ARROW.getValue());
                case ASTERISK -> parseOperatorWithDoubleAlternative(TokenType.ASTERISK,
                        '=', AutomatonState.ASTERISK_ASSIGN,
                        '*', AutomatonState.POWER);
                case ASTERISK_ASSIGN -> endToken(TokenType.ASTERISK_ASSIGN, TokenType.ASTERISK_ASSIGN.getValue());
                case POWER -> parseOperatorWithAlternative(TokenType.POWER,
                        '=', AutomatonState.POWER_ASSIGN);
                case POWER_ASSIGN -> endToken(TokenType.POWER_ASSIGN, TokenType.POWER_ASSIGN.getValue());
                case PERCENT -> parseOperatorWithAlternative(TokenType.PERCENT,
                        '=', AutomatonState.PERCENT_ASSIGN);
                case PERCENT_ASSIGN -> endToken(TokenType.PERCENT_ASSIGN, TokenType.PERCENT_ASSIGN.getValue());
                case SLASH -> parseOperatorWithAlternative(TokenType.SLASH,
                        '/', AutomatonState.DOUBLE_SLASH);
                case DOUBLE_SLASH -> endToken(TokenType.DOUBLE_SLASH, TokenType.DOUBLE_SLASH.getValue());
                case AT -> parseOperatorWithAlternative(TokenType.AT,
                        '=', AutomatonState.AT_ASSIGN);
                case AT_ASSIGN -> endToken(TokenType.AT_ASSIGN, TokenType.AT_ASSIGN.getValue());
                case LESS -> parseOperatorWithDoubleAlternative(TokenType.LESS,
                        '=', AutomatonState.LESS_EQUAL,
                        '<', AutomatonState.LEFT_SHIFT);
                case LESS_EQUAL -> endToken(TokenType.LESS_EQUAL, TokenType.LESS_EQUAL.getValue());
                case LEFT_SHIFT -> parseOperatorWithAlternative(TokenType.LEFT_SHIFT,
                        '=', AutomatonState.LEFT_SHIFT_ASSIGN);
                case LEFT_SHIFT_ASSIGN -> endToken(TokenType.LEFT_SHIFT_ASSIGN, TokenType.LEFT_SHIFT_ASSIGN.getValue());
                case GREATER -> parseOperatorWithDoubleAlternative(TokenType.GREATER,
                        '=', AutomatonState.GREATER_EQUAL,
                        '>', AutomatonState.RIGHT_SHIFT);
                case GREATER_EQUAL -> endToken(TokenType.GREATER_EQUAL, TokenType.GREATER_EQUAL.getValue());
                case RIGHT_SHIFT -> parseOperatorWithAlternative(TokenType.RIGHT_SHIFT,
                        '=', AutomatonState.RIGHT_SHIFT_ASSIGN);
                case RIGHT_SHIFT_ASSIGN -> endToken(TokenType.RIGHT_SHIFT_ASSIGN, TokenType.RIGHT_SHIFT_ASSIGN.getValue());
                case BITWISE_AND -> parseOperatorWithAlternative(TokenType.BITWISE_AND,
                        '=', AutomatonState.BITWISE_AND_ASSIGN);
                case BITWISE_AND_ASSIGN -> endToken(TokenType.BITWISE_AND_ASSIGN, TokenType.BITWISE_AND_ASSIGN.getValue());
                case BITWISE_OR -> parseOperatorWithAlternative(TokenType.BITWISE_OR,
                        '=', AutomatonState.BITWISE_OR_ASSIGN);
                case BITWISE_OR_ASSIGN -> endToken(TokenType.BITWISE_OR_ASSIGN, TokenType.BITWISE_OR_ASSIGN.getValue());
                case BITWISE_XOR -> parseOperatorWithAlternative(TokenType.BITWISE_XOR,
                        '=', AutomatonState.BITWISE_XOR_ASSIGN);
                case BITWISE_XOR_ASSIGN -> endToken(TokenType.BITWISE_XOR_ASSIGN, TokenType.BITWISE_XOR_ASSIGN.getValue());
                case BITWISE_NOT -> endToken(TokenType.BITWISE_NOT, TokenType.BITWISE_NOT.getValue());
                case ASSIGN -> parseOperatorWithAlternative(TokenType.ASSIGN,
                        '=', AutomatonState.EQUAL);
                case EQUAL -> endToken(TokenType.EQUAL, TokenType.EQUAL.getValue());
                case LEFT_PARENTHESIS -> endToken(TokenType.LEFT_PARENTHESIS, TokenType.LEFT_PARENTHESIS.getValue());
                case RIGHT_PARENTHESIS -> endToken(TokenType.RIGHT_PARENTHESIS, TokenType.RIGHT_PARENTHESIS.getValue());
                case LEFT_SQUARE_BRACKET -> endToken(TokenType.LEFT_SQUARE_BRACKET, TokenType.LEFT_SQUARE_BRACKET.getValue());
                case RIGHT_SQUARE_BRACKET -> endToken(TokenType.RIGHT_SQUARE_BRACKET, TokenType.RIGHT_SQUARE_BRACKET.getValue());
                case LEFT_CURLY_BRACKET -> endToken(TokenType.LEFT_CURLY_BRACKET, TokenType.LEFT_CURLY_BRACKET.getValue());
                case RIGHT_CURLY_BRACKET -> endToken(TokenType.RIGHT_CURLY_BRACKET, TokenType.RIGHT_CURLY_BRACKET.getValue());
                case COMMA -> endToken(TokenType.COMMA, TokenType.COMMA.getValue());
                case COLON -> parseOperatorWithAlternative(TokenType.COLON,
                        '=', AutomatonState.COLON_ASSIGN);
                case DOT -> parseDot();
                case COLON_ASSIGN -> endToken(TokenType.COLON_ASSIGN, TokenType.COLON_ASSIGN.getValue());
                case SEMICOLON -> endToken(TokenType.SEMICOLON, TokenType.SEMICOLON.getValue());
                case EXCLAMATION_MARK -> parseExclamationMark();
                case NOT_EQUAL -> endToken(TokenType.NOT_EQUAL, TokenType.NOT_EQUAL.getValue());
                case ZERO_INTEGER_OR_RADIX -> parseZeroIntegerOrRadix();
                case BINARY_INTEGER_START -> parseBinaryIntegerStart();
                case OCTAL_INTEGER_START -> parseOctalIntegerStart();
                case HEX_INTEGER_START -> parseHexIntegerStart();
                case BINARY_INTEGER -> parseBinaryInteger();
                case OCTAL_INTEGER -> parseOctalInteger();
                case HEX_INTEGER -> parseHexInteger();
                case DECIMAL_INTEGER -> parseDecimalInteger();
                case FLOAT -> parseFloat();
                case IMAGINARY -> endToken(TokenType.IMAGINARY_LITERAL, buffer.toString());
                case ZERO_INTEGER -> parseZeroInteger();
                case INTEGER_WITH_ZERO_PREFIX -> parseIntegerWithZeroPrefix();
                case EXPONENT_FLOAT_ON_INTEGER -> parseExponentFloatOnInteger();
                case EXPONENT_FLOAT_ON_ZERO_PREFIX_INTEGER -> parseExponentFloatOnZeroPrefixInteger();
                case EXPONENT_FLOAT_ON_FLOAT -> parseExponentFloatOnFloat();
                case SIGNED_EXPONENT_FLOAT_ON_INTEGER -> parseSignedExponentFloatOnInteger();
                case SIGNED_EXPONENT_FLOAT_ON_ZERO_PREFIX_INTEGER -> parseSignedExponentFloatOnZeroPrefixInteger();
                case SIGNED_EXPONENT_FLOAT_ON_FLOAT -> parseSignedExponentFloatOnFloat();
                case EXPONENT_FLOAT -> parseExponentFloat();
                case IDENTIFIER_OR_STRING_LITERAL -> parseIdentifierOrStringLiteral();
                case SINGLE_OR_TRIPLE_QUOTED_STRING -> parseSingleOrTripleQuotedString();
                case CLOSED_SINGLE_OR_OPENED_TRIPLE_QUOTED_STRING -> parseClosedSingleOrOpenedTripleQuotedString();
                case SINGLE_QUOTED_STRING -> parseSingleQuotedString();
                case DOUBLE_QUOTED_STRING -> parseDoubleQuotedString();
                case TRIPLE_QUOTED_STRING -> parseTripleQuotedString();
                case ESCAPE -> parseEscaped();
                case TRIPLE_QUOTED_STRING_WITH_QUOTE -> parseTripleQuotedStringWithQuote();
                case TRIPLE_QUOTED_STRING_WITH_DOUBLE_QUOTE -> parseTripleQuotedStringWithDoubleQuote();
                case FIRST_INDENT -> parseFirstIndent();
                case INDENT -> parseIndent();
                case BACKSLASH -> parseBackslash();
                case COMMENT -> skipComment();
            }
        }
    }

    private void startToken(AutomatonState state) {
        buffer.append(currentChar);
        this.state = state;
        tokenStartRow = currentLine;
        tokenStartColumn = currentColumn;
        blankLine = false;
    }

    private void endToken(TokenType type, String value) {
        tokens.add(new Token(type, value, tokenStartRow, tokenStartColumn));
        clearBufferAndSwitchState();
    }

    private void clearBufferAndSwitchState() {
        buffer.setLength(0);
        setStateByCurrentChar();
    }

    private void setStateByCurrentChar() {
        if (Utils.isValidIdentifierStart(currentChar)) {
            if (currentChar == 'U' || currentChar == 'u') {
                startToken(AutomatonState.IDENTIFIER_OR_STRING_LITERAL);
            } else {
                startToken(AutomatonState.KEYWORD_OR_IDENTIFIER);
            }
        } else if (currentChar == '+') {
            startToken(AutomatonState.PLUS);
        } else if (currentChar == '-') {
            startToken(AutomatonState.MINUS);
        } else if (currentChar == '*') {
            startToken(AutomatonState.ASTERISK);
        } else if (currentChar == '/') {
            startToken(AutomatonState.SLASH);
        } else if (currentChar == '%') {
            startToken(AutomatonState.PERCENT);
        } else if (currentChar == '@') {
            startToken(AutomatonState.AT);
        } else if (currentChar == '<') {
            startToken(AutomatonState.LESS);
        } else if (currentChar == '>') {
            startToken(AutomatonState.GREATER);
        } else if (currentChar == '&') {
            startToken(AutomatonState.BITWISE_AND);
        } else if (currentChar == '|') {
            startToken(AutomatonState.BITWISE_OR);
        } else if (currentChar == '^') {
            startToken(AutomatonState.BITWISE_XOR);
        } else if (currentChar == '~') {
            startToken(AutomatonState.BITWISE_NOT);
        } else if (currentChar == '=') {
            startToken(AutomatonState.ASSIGN);
        } else if (currentChar == '(') {
            startToken(AutomatonState.LEFT_PARENTHESIS);
        } else if (currentChar == ')') {
            startToken(AutomatonState.RIGHT_PARENTHESIS);
        } else if (currentChar == '[') {
            startToken(AutomatonState.LEFT_SQUARE_BRACKET);
        } else if (currentChar == ']') {
            startToken(AutomatonState.RIGHT_SQUARE_BRACKET);
        } else if (currentChar == '{') {
            startToken(AutomatonState.LEFT_CURLY_BRACKET);
        } else if (currentChar == '}') {
            startToken(AutomatonState.RIGHT_CURLY_BRACKET);
        } else if (currentChar == ',') {
            startToken(AutomatonState.COMMA);
        } else if (currentChar == '.') {
            startToken(AutomatonState.DOT);
        } else if (currentChar == ':') {
            startToken(AutomatonState.COLON);
        } else if (currentChar == ';') {
            startToken(AutomatonState.SEMICOLON);
        } else if (currentChar == '!') {
            startToken(AutomatonState.EXCLAMATION_MARK);
        } else if (Utils.isCorrectDigit(currentChar, 10)) {
            if (currentChar == '0') {
                startToken(AutomatonState.ZERO_INTEGER_OR_RADIX);
            } else {
                startToken(AutomatonState.DECIMAL_INTEGER);
            }
        } else if (currentChar == '\'') {
            startToken(AutomatonState.SINGLE_OR_TRIPLE_QUOTED_STRING);
            buffer.setLength(0);
        } else if (currentChar == '\"') {
            startToken(AutomatonState.DOUBLE_QUOTED_STRING);
            buffer.setLength(0);
        } else if (currentChar == '\n') {
            parseLineFeed();
        } else if (currentChar == '\\') {
            in.mark(Integer.MAX_VALUE);
            startToken(AutomatonState.BACKSLASH);
        } else if (Character.isWhitespace(currentChar)) {
            if (blankLine && (currentChar == ' ' || currentChar == '\t')) {
                state = AutomatonState.FIRST_INDENT;
            } else {
                state = AutomatonState.INITIAL;
            }
        } else if (currentChar == '#') {
            state = AutomatonState.COMMENT;
        } else {
            tokens.add(new Token(TokenType.ERROR, "Invalid symbol.", currentLine, currentColumn - 1));
            state = AutomatonState.INITIAL;
        }
    }

    private void parseKeywordOrIdentifier() {
        if (Character.isUnicodeIdentifierPart(currentChar)) {
            buffer.append(currentChar);
        } else {
            String value = buffer.toString();
            TokenType type = KEYWORDS.get(value);

            tokens.add(new Token(
                    Objects.requireNonNullElse(type, TokenType.IDENTIFIER), value, tokenStartRow, tokenStartColumn));
            clearBufferAndSwitchState();
        }
    }

    private void parseOperatorWithAlternative(TokenType type, char alternativeChar,
                                              AutomatonState alternativeState) {
        if (currentChar == alternativeChar) {
            buffer.append(currentChar);
            state = alternativeState;
        } else {
            endToken(type, type.getValue());
        }
    }

    private void parseOperatorWithDoubleAlternative(TokenType type,
                                                    char alternativeChar1, AutomatonState alternativeState1,
                                                    char alternativeChar2, AutomatonState alternativeState2) {
        if (currentChar == alternativeChar1) {
            buffer.append(currentChar);
            state = alternativeState1;
        } else if (currentChar == alternativeChar2) {
            buffer.append(currentChar);
            state = alternativeState2;
        } else {
            endToken(type, type.getValue());
        }
    }

    private void parseExclamationMark() {
        if (currentChar == '=') {
            buffer.append(currentChar);
            state = AutomatonState.NOT_EQUAL;
        } else {
            endToken(TokenType.ERROR, "Error. '!=' operator expected.");
        }
    }

    //=========================PROCESSING NUMERICAL LITERALS=========================

    private void parseDot() {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.FLOAT;
        } else {
            endToken(TokenType.DOT, TokenType.DOT.getValue());
        }
    }

    private void parseZeroIntegerOrRadix() {
        if (currentChar == 'B' || currentChar == 'b') {
            in.mark(2);
            buffer.append(currentChar);
            state = AutomatonState.BINARY_INTEGER_START;
        } else if (currentChar == 'O' || currentChar == 'o') {
            in.mark(2);
            buffer.append(currentChar);
            state = AutomatonState.OCTAL_INTEGER_START;
        } else if (currentChar == 'X' || currentChar == 'x') {
            in.mark(2);
            buffer.append(currentChar);
            state = AutomatonState.HEX_INTEGER_START;
        } else if (currentChar == 'E' || currentChar == 'e') {
            in.mark(3);
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT_ON_INTEGER;
        } else if (currentChar == 'J' || currentChar == 'j') {
            buffer.append(currentChar);
            state = AutomatonState.IMAGINARY;
        } else if (currentChar == '.') {
            buffer.append(currentChar);
            state = AutomatonState.FLOAT;
        } else if (currentChar == '0') {
            buffer.append(currentChar);
            state = AutomatonState.ZERO_INTEGER;
        } else if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.INTEGER_WITH_ZERO_PREFIX;
        } else {
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseBinaryIntegerStart() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 2)) {
            buffer.append(currentChar);
            state = AutomatonState.BINARY_INTEGER;
        } else {
            in.reset();
            currentColumn--;
            currentChar = buffer.charAt(buffer.length() - 1);
            buffer.delete(buffer.length() - 1, buffer.length());
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseOctalIntegerStart() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 8)) {
            buffer.append(currentChar);
            state = AutomatonState.OCTAL_INTEGER;
        } else {
            in.reset();
            currentColumn--;
            currentChar = buffer.charAt(buffer.length() - 1);
            buffer.delete(buffer.length() - 1, buffer.length());
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseHexIntegerStart() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 16)) {
            buffer.append(currentChar);
            state = AutomatonState.HEX_INTEGER;
        } else {
            in.reset();
            currentColumn--;
            currentChar = buffer.charAt(buffer.length() - 1);
            buffer.delete(buffer.length() - 1, buffer.length());
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseBinaryInteger() {
        if (Utils.isCorrectDigit(currentChar, 2)) {
            buffer.append(currentChar);
        } else {
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseOctalInteger() {
        if (Utils.isCorrectDigit(currentChar, 8)) {
            buffer.append(currentChar);
        } else {
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseHexInteger() {
        if (Utils.isCorrectDigit(currentChar, 16)) {
            buffer.append(currentChar);
        } else {
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseDecimalInteger() {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
        } else if (currentChar == '.') {
            buffer.append(currentChar);
            state = AutomatonState.FLOAT;
        } else if (currentChar == 'E' || currentChar == 'e') {
            in.mark(3);
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT_ON_INTEGER;
        } else if (currentChar == 'J' || currentChar == 'j') {
            buffer.append(currentChar);
            state = AutomatonState.IMAGINARY;
        } else {
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseFloat() {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
        } else if (currentChar == 'E' || currentChar == 'e') {
            in.mark(3);
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT_ON_FLOAT;
        } else if (currentChar == 'J' || currentChar == 'j') {
            buffer.append(currentChar);
            state = AutomatonState.IMAGINARY;
        } else {
            endToken(TokenType.FLOATING_POINT_LITERAL, buffer.toString());
        }
    }

    private void parseZeroInteger() {
        if (currentChar == '0') {
            buffer.append(currentChar);
        } else if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.INTEGER_WITH_ZERO_PREFIX;
        } else if (currentChar == '.') {
            buffer.append(currentChar);
            state = AutomatonState.FLOAT;
        } else if (currentChar == 'E' || currentChar == 'e') {
            in.mark(3);
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT_ON_INTEGER;
        } else if (currentChar == 'J' || currentChar == 'j') {
            buffer.append(currentChar);
            state = AutomatonState.IMAGINARY;
        } else {
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseIntegerWithZeroPrefix() {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
        } else if (currentChar == '.') {
            buffer.append(currentChar);
            state = AutomatonState.FLOAT;
        } else if (currentChar == 'E' || currentChar == 'e') {
            in.mark(3);
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT_ON_ZERO_PREFIX_INTEGER;
        } else if (currentChar == 'J' || currentChar == 'j') {
            buffer.append(currentChar);
            state = AutomatonState.IMAGINARY;
        } else {
            endToken(TokenType.ERROR, "Integer literal cannot start with 0.");
        }
    }

    private void parseExponentFloatOnInteger() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT;
        } else if (currentChar == '+' || currentChar == '-') {
            buffer.append(currentChar);
            state = AutomatonState.SIGNED_EXPONENT_FLOAT_ON_INTEGER;
        } else {
            in.reset();
            currentColumn--;
            currentChar = buffer.charAt(buffer.length() - 1);
            buffer.delete(buffer.length() - 1, buffer.length());
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseExponentFloatOnZeroPrefixInteger() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT;
        } else if (currentChar == '+' || currentChar == '-') {
            buffer.append(currentChar);
            state = AutomatonState.SIGNED_EXPONENT_FLOAT_ON_ZERO_PREFIX_INTEGER;
        } else {
            in.reset();
            currentColumn--;
            currentChar = buffer.charAt(buffer.length() - 1);
            buffer.delete(buffer.length() - 1, buffer.length());
            endToken(TokenType.ERROR, "Integer literal cannot start with 0.");
        }
    }

    private void parseExponentFloatOnFloat() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT;
        } else if (currentChar == '+' || currentChar == '-') {
            buffer.append(currentChar);
            state = AutomatonState.SIGNED_EXPONENT_FLOAT_ON_FLOAT;
        } else {
            in.reset();
            currentColumn--;
            currentChar = buffer.charAt(buffer.length() - 1);
            buffer.delete(buffer.length() - 1, buffer.length());
            endToken(TokenType.FLOATING_POINT_LITERAL, buffer.toString());
        }
    }

    private void parseSignedExponentFloatOnInteger() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT;
        } else {
            in.reset();
            currentColumn -= 2;
            currentChar = buffer.charAt(buffer.length() - 2);
            buffer.delete(buffer.length() - 2, buffer.length());
            endToken(TokenType.INTEGER_LITERAL, buffer.toString());
        }
    }

    private void parseSignedExponentFloatOnZeroPrefixInteger() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT;
        } else {
            in.reset();
            currentColumn -= 2;
            currentChar = buffer.charAt(buffer.length() - 2);
            buffer.delete(buffer.length() - 2, buffer.length());
            endToken(TokenType.ERROR, "Integer literal cannot start with 0.");
        }
    }

    private void parseSignedExponentFloatOnFloat() throws IOException {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
            state = AutomatonState.EXPONENT_FLOAT;
        } else {
            in.reset();
            currentColumn -= 2;
            currentChar = buffer.charAt(buffer.length() - 2);
            buffer.delete(buffer.length() - 2, buffer.length());
            endToken(TokenType.FLOATING_POINT_LITERAL, buffer.toString());
        }
    }

    private void parseExponentFloat() {
        if (Utils.isCorrectDigit(currentChar, 10)) {
            buffer.append(currentChar);
        } else {
            endToken(TokenType.FLOATING_POINT_LITERAL, buffer.toString());
        }
    }

    //==========================PROCESSING STRING LITERALS==========================

    private void parseIdentifierOrStringLiteral() {
        if (currentChar == '\'') {
            buffer.setLength(0);
            state = AutomatonState.SINGLE_OR_TRIPLE_QUOTED_STRING;
        } else if (currentChar == '\"') {
            buffer.setLength(0);
            state = AutomatonState.DOUBLE_QUOTED_STRING;
            currentStringType = StringType.DOUBLE_QUOTED;
        } else {
            state = AutomatonState.KEYWORD_OR_IDENTIFIER;
            parseKeywordOrIdentifier();
        }
    }

    private void parseSingleOrTripleQuotedString() {
        if (currentChar == '\'') {
            state = AutomatonState.CLOSED_SINGLE_OR_OPENED_TRIPLE_QUOTED_STRING;
        } else {
            state = AutomatonState.SINGLE_QUOTED_STRING;
            buffer.setLength(0);
            currentStringType = StringType.SINGLE_QUOTED;
            parseSingleQuotedString();
        }
    }

    private void parseClosedSingleOrOpenedTripleQuotedString() {
        buffer.setLength(0);
        if (currentChar == '\'') {
            state = AutomatonState.TRIPLE_QUOTED_STRING;
            currentStringType = StringType.TRIPLE_QUOTED;
        } else {
            currentStringType = StringType.NONE;
            endToken(TokenType.STRING_LITERAL, buffer.toString());
        }
    }

    private void parseSingleQuotedString() {
        if (currentChar == '\'') {
            quitString();
        } else if (currentChar == '\\') {
            state = AutomatonState.ESCAPE;
        } else if (currentChar == '\n') {
            endToken(TokenType.ERROR, "Missing closing single quote.");
            currentStringType = StringType.NONE;
        } else {
            buffer.append(currentChar);
        }
    }

    private void parseDoubleQuotedString() {
        if (currentChar == '\"') {
            quitString();
        } else if (currentChar == '\\') {
            state = AutomatonState.ESCAPE;
        } else if (currentChar == '\n') {
            endToken(TokenType.ERROR, "Missing closing double quote.");
            currentStringType = StringType.NONE;
        } else {
            buffer.append(currentChar);
        }
    }

    private void parseTripleQuotedString() {
        if (currentChar == '\'') {
            buffer.append(currentChar);
            state = AutomatonState.TRIPLE_QUOTED_STRING_WITH_QUOTE;
        } else if (currentChar == '\\') {
            state = AutomatonState.ESCAPE;
        } else {
            if (currentChar == '\n') {
                currentLine++;
            }
            buffer.append(currentChar);
        }
    }

    private void parseTripleQuotedStringWithQuote() {
        if (currentChar == '\'') {
            buffer.append(currentChar);
            state = AutomatonState.TRIPLE_QUOTED_STRING_WITH_DOUBLE_QUOTE;
        } else {
            state = AutomatonState.TRIPLE_QUOTED_STRING;
            parseSingleQuotedString();
        }
    }

    private void parseTripleQuotedStringWithDoubleQuote() {
        if (currentChar == '\'') {
            buffer.delete(buffer.length() - 2, buffer.length());
            quitString();
        } else {
            state = AutomatonState.TRIPLE_QUOTED_STRING;
            parseSingleQuotedString();
        }
    }

    private void quitString() {
        tokens.add(new Token(TokenType.STRING_LITERAL, buffer.toString(), tokenStartRow, tokenStartColumn));
        buffer.setLength(0);
        state = AutomatonState.INITIAL;
        currentStringType = StringType.NONE;
    }

    private void parseEscaped() {
        Character escaped = Utils.escapeChar(currentChar);

        if (escaped != null) {
            buffer.append(escaped);
        } else {
            buffer.append('\\' + currentChar);
        }
        switch (currentStringType) {
            case SINGLE_QUOTED -> state = AutomatonState.SINGLE_QUOTED_STRING;
            case DOUBLE_QUOTED -> state = AutomatonState.DOUBLE_QUOTED_STRING;
            case TRIPLE_QUOTED -> state = AutomatonState.TRIPLE_QUOTED_STRING;
        }
    }

    //================================PROCESSING LINE================================

    private void parseLineFeed() {
        if (!blankLine) {
            tokens.add(new Token(TokenType.NEWLINE, TokenType.NEWLINE.getValue(), currentLine, currentColumn));
            currentIndent = 0;
            blankLine = true;
            state = AutomatonState.INDENT;
        } else if (tokens.isEmpty()) {
            state = AutomatonState.INITIAL;
        } else {
            state = AutomatonState.INDENT;
        }

        currentLine++;
        currentColumn = -1;
    }

    private void parseFirstIndent() {
        if (!Character.isWhitespace(currentChar)) {
            if (currentChar == '#') {
                setStateByCurrentChar();
            } else {
                tokens.add(new Token(TokenType.ERROR, "Unexpected indent.", currentLine, currentColumn - 1));
                blankLine = false;
                setStateByCurrentChar();
            }
        } else if (currentChar == '\n') {
            currentLine++;
            currentColumn = -1;
            setStateByCurrentChar();
        }
    }

    private void parseIndent() {
        if (Character.isWhitespace(currentChar)) {
            if (currentChar == ' ') {
                currentIndent++;
            } else if (currentChar == '\t') {
                currentIndent += (TAB_STOP_LENGTH - (currentIndent % TAB_STOP_LENGTH)) % TAB_STOP_LENGTH;
            } else if (currentChar == '\n') {
                currentIndent = 0;
                setStateByCurrentChar();
            }
        } else if (currentChar == '\\') {
            blankLine = false;
            setStateByCurrentChar();
        } else if (currentChar == '#') {
            setStateByCurrentChar();
        } else {
            if (currentIndent > indentsStack.get(indentsStack.size() - 1)) {
                indentsStack.add(currentIndent);
                tokens.add(new Token(TokenType.INDENT, TokenType.INDENT.getValue(), currentLine, currentIndent));
            } else if (currentIndent < indentsStack.getLast()) {
                if (indentsStack.contains(currentIndent)) {
                    while (indentsStack.getLast() > currentIndent) {
                        int popped = indentsStack.removeLast();
                        tokens.add(new Token(TokenType.DEDENT, TokenType.DEDENT.getValue(), currentLine, popped));
                    }
                } else {
                    tokens.add(new Token(TokenType.ERROR, "Unindent does not match to any outer indentation level.",
                            currentLine, currentIndent));
                }
            }
            blankLine = false;
            setStateByCurrentChar();
        }
    }

    private void parseBackslash() throws IOException {
        if (!Character.isWhitespace(currentChar)) {
            in.reset();
            currentColumn = tokenStartColumn + 1;
            tokens.add(new Token(TokenType.ERROR, "Backslash does not continue a line.", tokenStartRow,
                    tokenStartColumn));
            buffer.setLength(0);
            state = AutomatonState.INITIAL;
        } else if (currentChar == '\n') {
            buffer.setLength(0);
            state = AutomatonState.INITIAL;
            currentLine++;
        }
    }

    private void skipComment() {
        if (currentChar == '\n') {
            setStateByCurrentChar();
        }
    }
}
