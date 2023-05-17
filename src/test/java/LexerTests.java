import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Lexer Class
 * @author Bella Mester
 */
public class LexerTests {

    @Test
    public void lexerTestStub() {
        String source = "int x = 10;";
        Lexer lexer   = new Lexer(source);

        Lexer.Token token1 = lexer.getToken();
        assertEquals(Lexer.TokenType.Identifier, token1.tokentype);
        assertEquals("int", token1.value);

        Lexer.Token token2 = lexer.getToken();
        assertEquals(Lexer.TokenType.Identifier, token2.tokentype);
        assertEquals("x", token2.value);

        Lexer.Token token3 = lexer.getToken();
        assertEquals(Lexer.TokenType.Op_assign, token3.tokentype);
        assertEquals("", token3.value);

        Lexer.Token token4 = lexer.getToken();
        assertEquals(Lexer.TokenType.Integer, token4.tokentype);
        assertEquals("10", token4.value);

        Lexer.Token token5 = lexer.getToken();
        assertEquals(Lexer.TokenType.Semicolon, token5.tokentype);
        assertEquals("", token5.value);

        Lexer.Token token6 = lexer.getToken();
        assertEquals(Lexer.TokenType.End_of_input, token6.tokentype);
        assertEquals("", token6.value);
    }

    @Test
    public void testPrintTokens() {
        String source = "int x = 10;";
        Lexer lexer   = new Lexer(source);

        String result = lexer.printTokens();

        String expect = """
                1      1 Identifier      int
                1      5 Identifier      x
                1      7 Op_assign
                 1      9 Integer          10
                 1     11 Semicolon
                 1     11 End_of_input""".indent(4);

        assertEquals(expect, result);

    }
}
