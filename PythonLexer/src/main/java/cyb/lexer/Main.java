package cyb.lexer;

import java.io.*;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream("src/main/resources/main1.py"));
            Lexer lexer = new Lexer(in);
            List<Token> tokens = lexer.analyze();
            int currentLine = -1;
            for (Token token : tokens) {
                if (token.getLine() != currentLine) {
                    System.out.println();
                    currentLine = token.getLine();
                    System.out.print(currentLine + ":     ");
                }
                System.out.print("(" + token.getType().toString() + ", " + token.getValue() + ")  ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
