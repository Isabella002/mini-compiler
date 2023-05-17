import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Syntax Analyzer
 * @author Nick Trimmer
 */
class Parser {

    private final List<Token> source;
    private Token token;
    private int position;

    Parser(List<Token> source) {
        this.source = source;
        this.token = null;
        this.position = 0;
    }

    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }

    Token getNextToken() {
        this.token = this.source.get(this.position++);
        return this.token;
    }

    public Node expr(int precedence) {
        Node exprNode = null;  // This will be the node representing the expression
        Node tempNode = null;  // Temporary node for intermediate operations

        // Check the type of the current token and handle accordingly
        if (this.token.tokentype == TokenType.LeftParen) {
            // Handle expressions in parentheses
            exprNode = paren_expr();
        } else if (this.token.tokentype == TokenType.Op_subtract || this.token.tokentype == TokenType.Op_add) {
            // Handle addition and subtraction
            // Determine the operation type (negation or addition)
            int operationType = (this.token.tokentype.getId() == TokenType.Op_subtract.getId())
                    ? TokenType.Op_negate.getId()
                    : TokenType.Op_add.getId();
            getNextToken();
            // Parse the expression after the operation
            tempNode = expr(TokenType.Op_negate.precedence);
            // Create a node representing the operation
            exprNode = (operationType == TokenType.Op_negate.getId())
                    ? Node.make_node(NodeType.nd_Negate, tempNode)
                    : tempNode;
        } else if (this.token.tokentype == TokenType.Op_not) {
            // Handle the 'not' operation
            getNextToken();
            tempNode = expr(TokenType.Op_not.precedence);
            exprNode = Node.make_node(NodeType.nd_Not, tempNode);
        } else if (this.token.tokentype == TokenType.Identifier) {
            // Handle identifiers
            exprNode = Node.make_leaf(NodeType.nd_Ident, this.token.value);
            getNextToken();
        } else if (this.token.tokentype == TokenType.Integer) {
            // Handle integers
            exprNode = Node.make_leaf(NodeType.nd_Integer, this.token.value);
            getNextToken();
        } else if (this.token.tokentype == TokenType.String) {
            // Handle strings
            exprNode = Node.make_leaf(NodeType.nd_String, this.token.value);
            getNextToken();
        } else {
            // If the token type is none of the above, throw an error
            error(this.token.line, this.token.pos, "Unexpected token: " + this.token.tokentype);
        }

        // Handle binary operations (operations with two operands)
        while (this.token.tokentype.isBinary() && this.token.tokentype.getPrecedence() >= precedence) {
            int operationType = this.position;
            getNextToken();
            // Parse the expression after the operation
            tempNode = expr(TokenType.get(operationType).getPrecedence()
                    + (this.token.tokentype.isRightAssoc() ? 0 : 1));
            // Create a node representing the operation and its operands
            exprNode = Node.make_node(TokenType.get(operationType).getNodeType(), exprNode, tempNode);
        }
        return exprNode;
    }

    public Node paren_expr() {
        getNextToken(); // consume '('
        Node x = expr(TokenType.Comma.getPrecedence()); // parse the first expression

        // while there are more comma-separated expressions, keep parsing
        while (this.token.tokentype == TokenType.Comma) {
            getNextToken(); // consume ','
            Node y = expr(TokenType.Comma.getPrecedence()); // parse the next expression
            x = Node.make_node(TokenType.Comma.getNodeType(), x, y); // combine the expressions
        }

        // after parsing all comma-separated expressions, expect a ')'
        if (this.token.tokentype != TokenType.RightParen) {
            error(this.token.line, this.token.pos, "Expecting 'RightParen', found: " + this.token.tokentype);
        }

        getNextToken(); // consume ')'
        return x;
    }

    void expect(String msg, TokenType s) {
        if (this.token.tokentype == s) {
            getNextToken();
            return;
        }
        error(this.token.line, this.token.pos, msg + ": Expecting '" + s + "', found: '" + this.token.tokentype + "'");
    }

    public Node stmt() {
        Node statementNode = null;   // This will be the node representing the statement
        Node trueBranchNode, falseBranchNode, exprNode, variableNode;

        // Check the type of the current token and handle accordingly
        if (this.token.tokentype == TokenType.Keyword_if) {
            // Handle 'if' statements
            getNextToken();
            exprNode = paren_expr();  // Parse the condition expression in parentheses
            trueBranchNode = stmt();  // Parse the true branch of the 'if' statement
            falseBranchNode = null;   // Initialize the false branch node to null
            if (this.token.tokentype == TokenType.Keyword_else) {
                // If there is an 'else' keyword, parse the false branch of the 'if' statement
                getNextToken();
                falseBranchNode = stmt();
            }
            statementNode = Node.make_node(NodeType.nd_If, exprNode, Node.make_node(NodeType.nd_If, trueBranchNode, falseBranchNode));
        }
        else if (this.token.tokentype == TokenType.Keyword_putc) {
            // Handle 'putc' statements
            getNextToken();
            exprNode = paren_expr();  // Parse the expression to be printed
            statementNode = Node.make_node(NodeType.nd_Prtc, exprNode);
            expect("Putc", TokenType.Semicolon);
        }
        else if (this.token.tokentype == TokenType.Keyword_print) {
            // Handle 'print' statements
            getNextToken();
            exprNode = paren_expr();  // Parse the expression to be printed
            statementNode = Node.make_node(NodeType.nd_Prti, exprNode);
            expect("Print", TokenType.Semicolon);
        }
        else if (this.token.tokentype == TokenType.Keyword_while) {
            // Handle 'while' loops
            getNextToken();
            exprNode = paren_expr();  // Parse the loop condition
            trueBranchNode = stmt();  // Parse the body of the loop
            statementNode = Node.make_node(NodeType.nd_While, exprNode, trueBranchNode);
        }
        else if (this.token.tokentype == TokenType.LeftBrace) {
            // Handle block of statements
            getNextToken();
            statementNode = stmt();   // Parse the first statement in the block

            // Keep parsing statements until a right brace is encountered
            while (this.token.tokentype != TokenType.RightBrace && this.token.tokentype != TokenType.End_of_input) {
                statementNode = Node.make_node(NodeType.nd_Sequence, statementNode, stmt());
            }

            expect("LeftBrace", TokenType.RightBrace);
        }
        else if (this.token.tokentype == TokenType.Identifier) {
            // Handle assignments
            variableNode = Node.make_leaf(NodeType.nd_Ident, this.token.value);  // Create a node for the variable
            getNextToken();
            expect("Identifier", TokenType.Op_assign);  // Expect an assignment operator
            exprNode = expr(0);  // Parse the expression to be assigned
            statementNode = Node.make_node(NodeType.nd_Assign, variableNode, exprNode);
            expect("Assign", TokenType.Semicolon);
        }
        else if (this.token.tokentype == TokenType.End_of_input) {
            // If the end of input is reached, do nothing
        }
        else {
            // If the token type is none of the above, throw an error
            error(this.token.line, this.token.pos, "Expecting start of statement, found: " + this.token.tokentype);
        }

        return statementNode;
    }



    Node parse() {
        Node t = null;
        getNextToken();
        while (this.token.tokentype != TokenType.End_of_input) {
            t = Node.make_node(NodeType.nd_Sequence, t, stmt());
        }
        return t;
    }

    String printAST(Node t, StringBuilder sb) {
        int i = 0;
        if (t == null) {
            sb.append(";");
            sb.append("\n");
            System.out.println(";");
        } else {
            sb.append(t.nt);
            System.out.printf("%-14s", t.nt);
            if (t.nt == NodeType.nd_Ident || t.nt == NodeType.nd_Integer || t.nt == NodeType.nd_String) {
                sb.append(" " + t.value);
                sb.append("\n");
                System.out.println(" " + t.value);
            } else {
                sb.append("\n");
                System.out.println();
                printAST(t.left, sb);
                printAST(t.right, sb);
            }

        }
        return sb.toString();
    }

    static void outputToFile(String result) {
        try {
            FileWriter myWriter = new FileWriter("src/main/resources/hello.par");
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class Node {

        public NodeType nt;
        public Node left, right;
        public String value;

        Node() {
            this.nt = null;
            this.left = null;
            this.right = null;
            this.value = null;
        }

        Node(NodeType node_type, Node left, Node right, String value) {
            this.nt = node_type;
            this.left = left;
            this.right = right;
            this.value = value;
        }

        public static Node make_node(NodeType nodetype, Node left, Node right) {
            return new Node(nodetype, left, right, "");
        }

        public static Node make_node(NodeType nodetype, Node left) {
            return new Node(nodetype, left, null, "");
        }

        public static Node make_leaf(NodeType nodetype, String value) {
            return new Node(nodetype, null, null, value);
        }
    }

    static enum NodeType {

        nd_None(""), nd_Ident("Identifier"), nd_String("String"),
        nd_Integer("Integer"), nd_Sequence("Sequence"), nd_If("If"),
        nd_Prtc("Prtc"), nd_Prts("Prts"), nd_Prti("Prti"),
        nd_While("While"), nd_Assign("Assign"), nd_Negate("Negate"),
        nd_Not("Not"), nd_Mul("Multiply"), nd_Div("Divide"),
        nd_Mod("Mod"), nd_Add("Add"), nd_Sub("Subtract"),
        nd_Lss("Less"), nd_Leq("LessEqual"), nd_Gtr("Greater"),
        nd_Geq("GreaterEqual"), nd_Eql("Equal"), nd_Neq("NotEqual"),
        nd_And("And"), nd_Or("Or");

        private final String name;

        NodeType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    static class Token {

        public TokenType tokentype;
        public String value;
        public int line;
        public int pos;

        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }

        @Override
        public String toString() {
            return String.format("%5d  %5d %-15s %s", this.line, this.pos, this.tokentype, this.value);
        }
    }

    static enum TokenType {

        End_of_input(0, false, false, false, -1, NodeType.nd_None),
        Op_multiply(1, false, true, false, 13, NodeType.nd_Mul),
        Op_divide(2, false, true, false, 13, NodeType.nd_Div),
        Op_mod(3, false, true, false, 13, NodeType.nd_Mod),
        Op_add(4, false, true, false, 12, NodeType.nd_Add),
        Op_subtract(5, false, true, false, 12, NodeType.nd_Sub),
        Op_negate(6, false, false, true, 14, NodeType.nd_Negate),
        Op_not(7, false, false, true, 14, NodeType.nd_Not),
        Op_less(8, false, true, false, 10, NodeType.nd_Lss),
        Op_lessequal(9, false, true, false, 10, NodeType.nd_Leq),
        Op_greater(10, false, true, false, 10, NodeType.nd_Gtr),
        Op_greaterequal(11, false, true, false, 10, NodeType.nd_Geq),
        Op_equal(12, false, true, true, 9, NodeType.nd_Eql),
        Op_notequal(13, false, true, false, 9, NodeType.nd_Neq),
        Op_assign(14, false, false, false, -1, NodeType.nd_Assign),
        Op_and(15, false, true, false, 5, NodeType.nd_And),
        Op_or(16, false, true, false, 4, NodeType.nd_Or),
        Keyword_if(17, false, false, false, -1, NodeType.nd_If),
        Keyword_else(18, false, false, false, -1, NodeType.nd_None),
        Keyword_while(19, false, false, false, -1, NodeType.nd_While),
        Keyword_print(20, false, false, false, -1, NodeType.nd_None),
        Keyword_putc(21, false, false, false, -1, NodeType.nd_None),
        LeftParen(22, false, false, false, -1, NodeType.nd_None),
        RightParen(23, false, false, false, -1, NodeType.nd_None),
        LeftBrace(24, false, false, false, -1, NodeType.nd_None),
        RightBrace(25, false, false, false, -1, NodeType.nd_None),
        Semicolon(26, false, false, false, -1, NodeType.nd_None),
        Comma(27, false, false, false, -1, NodeType.nd_Prti),
        Identifier(28, false, false, false, -1, NodeType.nd_Ident),
        Integer(29, false, false, false, -1, NodeType.nd_Integer),
        String(30, false, false, false, -1, NodeType.nd_String);

        private final int id;
        private final int precedence;
        private final boolean right_assoc;
        private final boolean is_binary;
        private final boolean is_unary;
        private final NodeType node_type;

        TokenType(int id, boolean right_assoc, boolean is_binary, boolean is_unary, int precedence, NodeType node) {
            this.id = id;
            this.right_assoc = right_assoc;
            this.is_binary = is_binary;
            this.is_unary = is_unary;
            this.precedence = precedence;
            this.node_type = node;
        }

        public int getId() {
            return this.id;
        }

        boolean isRightAssoc() {
            return this.right_assoc;
        }

        boolean isBinary() {
            return this.is_binary;
        }

        boolean isUnary() {
            return this.is_unary;
        }

        int getPrecedence() {
            return this.precedence;
        }

        NodeType getNodeType() {
            return this.node_type;
        }

        public static TokenType get(int id) {
            for (TokenType type : TokenType.values()) {
                if (type.getId() == id) {
                    return type;
                }
            }
            return null;
        }
    }

    public static void main(String[] args) {
        if (1==1) {
            try {
                String value, token;
                String result = " ";
                StringBuilder sb = new StringBuilder();
                int line, pos;
                Token t;
                boolean found;
                List<Token> list = new ArrayList<>();
                Map<String, TokenType> str_to_tokens = new HashMap<>();
                str_to_tokens.put("End_of_input", TokenType.End_of_input);
                str_to_tokens.put("Op_multiply", TokenType.Op_multiply);
                str_to_tokens.put("Op_divide", TokenType.Op_divide);
                str_to_tokens.put("Op_mod", TokenType.Op_mod);
                str_to_tokens.put("Op_add", TokenType.Op_add);
                str_to_tokens.put("Op_subtract", TokenType.Op_subtract);
                str_to_tokens.put("Op_negate", TokenType.Op_negate);
                str_to_tokens.put("Op_not", TokenType.Op_not);
                str_to_tokens.put("Op_less", TokenType.Op_less);
                str_to_tokens.put("Op_lessequal", TokenType.Op_lessequal);
                str_to_tokens.put("Op_greater", TokenType.Op_greater);
                str_to_tokens.put("Op_greaterequal", TokenType.Op_greaterequal);
                str_to_tokens.put("Op_equal", TokenType.Op_equal);
                str_to_tokens.put("Op_notequal", TokenType.Op_notequal);
                str_to_tokens.put("Op_assign", TokenType.Op_assign);
                str_to_tokens.put("Op_and", TokenType.Op_and);
                str_to_tokens.put("Op_or", TokenType.Op_or);
                str_to_tokens.put("Keyword_if", TokenType.Keyword_if);
                str_to_tokens.put("Keyword_else", TokenType.Keyword_else);
                str_to_tokens.put("Keyword_while", TokenType.Keyword_while);
                str_to_tokens.put("Keyword_print", TokenType.Keyword_print);
                str_to_tokens.put("Keyword_putc", TokenType.Keyword_putc);
                str_to_tokens.put("LeftParen", TokenType.LeftParen);
                str_to_tokens.put("RightParen", TokenType.RightParen);
                str_to_tokens.put("LeftBrace", TokenType.LeftBrace);
                str_to_tokens.put("RightBrace", TokenType.RightBrace);
                str_to_tokens.put("Semicolon", TokenType.Semicolon);
                str_to_tokens.put("Comma", TokenType.Comma);
                str_to_tokens.put("Identifier", TokenType.Identifier);
                str_to_tokens.put("Integer", TokenType.Integer);
                str_to_tokens.put("String", TokenType.String);
                Scanner s = new Scanner(new File("src/main/resources/count.lex"));
                String source = " ";
                while (s.hasNext()) {
                    String str = s.nextLine();
                    StringTokenizer st = new StringTokenizer(str);
                    line = Integer.parseInt(st.nextToken());
                    pos = Integer.parseInt(st.nextToken());
                    token = st.nextToken();
                    value = "";
                    while (st.hasMoreTokens()) {
                        value += st.nextToken() + " ";
                    }
                    found = false;
                    if (str_to_tokens.containsKey(token)) {
                        found = true;
                        list.add(new Token(str_to_tokens.get(token), value, line, pos));
                    }
                    if (found == false) {
                        throw new Exception("Token not found: '" + token + "'");
                    }
                }
                Parser p = new Parser(list);
                result = p.printAST(p.parse(), sb);
                outputToFile(result);
            } catch (FileNotFoundException e) {
                error(-1, -1, "Exception: " + e.getMessage());
            } catch (Exception e) {
                error(-1, -1, "Exception: " + e.getMessage());
            }
        } else {
            error(-1, -1, "No args");
        }
    }
}