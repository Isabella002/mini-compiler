import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

public class ParserTests {

    @Test
    public void testExprInteger() {
        List<Parser.Token> tokens = Arrays.asList(new Parser.Token(Parser.TokenType.Integer, "42", 0, 0));
        Parser parser = new Parser(tokens);
        Parser.Node result = parser.expr(0);
        assertEquals(Parser.NodeType.nd_Integer, result.nt);
        assertEquals("42", result.value);
    }

    @Test
    public void testExprIdentifier() {
        List<Parser.Token> tokens = Arrays.asList(new Parser.Token(Parser.TokenType.Identifier, "x", 0, 0));
        Parser parser = new Parser(tokens);
        Parser.Node result = parser.expr(0);
        assertEquals(Parser.NodeType.nd_Ident, result.nt);
        assertEquals("x", result.value);
    }

    @Test
    public void testStmtAssign() {
        List<Parser.Token> tokens = Arrays.asList(
                new Parser.Token(Parser.TokenType.Identifier, "x", 0, 0),
                new Parser.Token(Parser.TokenType.Op_assign, "=", 0, 1),
                new Parser.Token(Parser.TokenType.Integer, "42", 0, 2),
                new Parser.Token(Parser.TokenType.Semicolon, ";", 0, 4)
        );
        Parser parser = new Parser(tokens);
        Parser.Node result = parser.stmt();
        assertEquals(Parser.NodeType.nd_Assign, result.nt);
        assertEquals(Parser.NodeType.nd_Ident, result.left.nt);
        assertEquals("x", result.left.value);
        assertEquals(Parser.NodeType.nd_Integer, result.right.nt);
        assertEquals("42", result.right.value);
    }
}
