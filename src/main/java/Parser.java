import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * The Parser class is responsible for parsing a list of tokens from a source code file.
 * It is built on the principles of a recursive descent parser and generates an Abstract Syntax Tree (AST)
 * representation of the input source code.
 * The parser processes the tokens and constructs an AST based on the grammar rules defined for the
 * language being parsed. It also reports syntax errors in the source code file.
 */
class Parser {

    private final List<Token> source;
    private Token token;
    private int position;

    /**
     * The Node class represents a node in the abstract syntax tree.
     * Each node has a type, possibly left and right child nodes, and a value.
     */
    static class Node {

        /**
         * The type of this node.
         */
        public NodeType nodeType;

        /**
         * The left child of this node.
         */
        public Node leftChild;

        /**
         * The right child of this node.
         */
        public Node rightChild;

        /**
         * The value associated with this node.
         */
        public String nodeValue;

        /**
         * Default constructor to initialize a new Node object with null values.
         */
        Node() {
            this.nodeType = null;
            this.leftChild = null;
            this.rightChild = null;
            this.nodeValue = null;
        }

        /**
         * Constructor to initialize a new Node object.
         *
         * @param nodeType The type of the node.
         * @param leftChild The left child node.
         * @param rightChild The right child node.
         * @param nodeValue The value associated with the node.
         */
        Node(NodeType nodeType, Node leftChild, Node rightChild, String nodeValue) {
            this.nodeType = nodeType;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.nodeValue = nodeValue;
        }

        /**
         * Static factory method to create a new node with the specified type, left and right children.
         *
         * @param nodeType The type of the new node.
         * @param leftChild The left child of the new node.
         * @param rightChild The right child of the new node.
         * @return The newly created Node object.
         */
        public static Node makeNode(NodeType nodeType, Node leftChild, Node rightChild) {
            return new Node(nodeType, leftChild, rightChild, "");
        }

        /**
         * Static factory method to create a new node with the specified type and left child.
         *
         * @param nodeType The type of the new node.
         * @param leftChild The left child of the new node.
         * @return The newly created Node object.
         */
        public static Node makeNode(NodeType nodeType, Node leftChild) {
            return new Node(nodeType, leftChild, null, "");
        }

        /**
         * Static factory method to create a new leaf node (node without children) with the specified type and value.
         *
         * @param nodeType The type of the new node.
         * @param nodeValue The value of the new node.
         * @return The newly created Node object.
         */
        public static Node makeLeaf(NodeType nodeType, String nodeValue) {
            return new Node(nodeType, null, null, nodeValue);
        }
    }

    /**
     * The Token class encapsulates the details of a token from the input source.
     * Each token has a type, a value, and details of its position (line and column) in the source.
     */
    static class Token {

        /**
         * The type of this token.
         */
        public TokenType tokenType;

        /**
         * The actual text value of this token.
         */
        public String tokenValue;

        /**
         * The line number in the source where this token starts.
         */
        public int lineNumber;

        /**
         * The column number in the source where this token starts.
         */
        public int columnNumber;

        /**
         * Constructor to initialize a new Token object.
         *
         * @param tokenType The type of the token.
         * @param tokenValue The text value of the token.
         * @param lineNumber The line number in the source where the token starts.
         * @param columnNumber The column number in the source where the token starts.
         */
        Token(TokenType tokenType, String tokenValue, int lineNumber, int columnNumber) {
            this.tokenType = tokenType;
            this.tokenValue = tokenValue;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        /**
         * This method provides a string representation of the Token object.
         *
         * @return A string representation of the Token object.
         */
        @Override
        public String toString() {
            return String.format("%5d  %5d %-15s %s", this.lineNumber, this.columnNumber, this.tokenType, this.tokenValue);
        }
    }

    /**
     * TokenType is an enumeration that defines various types of tokens that can be present in the source code.
     */
    static enum TokenType {
        // Define each token type along with its properties.
        End_of_input(false, false, false, -1, NodeType.None),
        Op_multiply(false, true, false, 13, NodeType.Multiply),
        Op_divide(false, true, false, 13, NodeType.Divide),
        Op_mod(false, true, false, 13, NodeType.Mod),
        Op_add(false, true, false, 12, NodeType.Add),
        Op_subtract(false, true, false, 12, NodeType.Subtract),
        Op_negate(false, false, true, 14, NodeType.Negate),
        Op_not(false, false, true, 14, NodeType.Not),
        Op_less(false, true, false, 10, NodeType.Less),
        Op_lessequal(false, true, false, 10, NodeType.LessEqual),
        Op_greater(false, true, false, 10, NodeType.Greater),
        Op_greaterequal(false, true, false, 10, NodeType.GreaterEqual),
        Op_equal(false, true, true, 9, NodeType.Equal),
        Op_notequal(false, true, false, 9, NodeType.NotEqual),
        Op_assign(false, false, false, -1, NodeType.Assign),
        Op_and(false, true, false, 5, NodeType.And),
        Op_or(false, true, false, 4, NodeType.Or),
        Keyword_if(false, false, false, -1, NodeType.If),
        Keyword_else(false, false, false, -1, NodeType.None),
        Keyword_while(false, false, false, -1, NodeType.While),
        Keyword_print(false, false, false, -1, NodeType.None),
        Keyword_putc(false, false, false, -1, NodeType.None),
        LeftParen(false, false, false, -1, NodeType.None),
        RightParen(false, false, false, -1, NodeType.None),
        LeftBrace(false, false, false, -1, NodeType.None),
        RightBrace(false, false, false, -1, NodeType.None),
        Semicolon(false, false, false, -1, NodeType.None),
        Comma(false, false, false, -1, NodeType.None),
        Identifier(false, false, false, -1, NodeType.Identifier),
        Integer(false, false, false, -1, NodeType.Integer),
        String(false, false, false, -1, NodeType.String);

        private final int precedenceValue;
        private final boolean isRightAssociative;
        private final boolean isBinaryOperator;
        private final boolean isUnaryOperator;
        private final NodeType correspondingNodeType;

        /**
         * Constructor for TokenType.
         *
         * @param isRightAssociative  Specifies if the operator is right associative.
         * @param isBinaryOperator    Specifies if the operator is binary.
         * @param isUnaryOperator     Specifies if the operator is unary.
         * @param precedenceValue     The precedence value of the operator.
         * @param correspondingNodeType The NodeType corresponding to this token.
         */
        TokenType(boolean isRightAssociative, boolean isBinaryOperator, boolean isUnaryOperator,
                  int precedenceValue, NodeType correspondingNodeType) {
            this.isRightAssociative = isRightAssociative;
            this.isBinaryOperator = isBinaryOperator;
            this.isUnaryOperator = isUnaryOperator;
            this.precedenceValue = precedenceValue;
            this.correspondingNodeType = correspondingNodeType;
        }

        /**
         * Checks if the operator is right associative.
         *
         * @return True if the operator is right associative, false otherwise.
         */
        boolean isRightAssociative() {
            return this.isRightAssociative;
        }

        /**
         * Checks if the operator is binary.
         *
         * @return True if the operator is binary, false otherwise.
         */
        boolean isBinaryOperator() {
            return this.isBinaryOperator;
        }

        /**
         * Checks if the operator is unary.
         *
         * @return True if the operator is unary, false otherwise.
         */
        boolean isUnaryOperator() {
            return this.isUnaryOperator;
        }

        /**
         * Gets the precedence value of the operator.
         *
         * @return The precedence value.
         */
        int getPrecedenceValue() {
            return this.precedenceValue;
        }

        /**
         * Gets the NodeType corresponding to this token.
         *
         * @return The corresponding NodeType.
         */
        NodeType getCorrespondingNodeType() {
            return this.correspondingNodeType;
        }
    }

    /**
     * Represents the different types of nodes that can be present in an abstract syntax tree (AST).
     * Each enum value corresponds to a different type of node, and includes a human-readable name
     * for the node type that can be retrieved using the toString method.
     */
    public enum NodeType {
        None("None"),
        Identifier("Identifier"),
        String("String"),
        Integer("Integer"),
        Sequence("Sequence"),
        If("If"),
        PrintCharacter("Prtc"),
        PrintString("Prts"),
        PrintInteger("Prti"),
        While("While"),
        Assign("Assign"),
        Negate("Negate"),
        Not("Not"),
        Multiply("Multiply"),
        Divide("Divide"),
        Mod("Mod"),
        Add("Add"),
        Subtract("Subtract"),
        Less("Less"),
        LessEqual("LessEqual"),
        Greater("Greater"),
        GreaterEqual("GreaterEqual"),
        Equal("Equal"),
        NotEqual("NotEqual"),
        And("And"),
        Or("Or");

        private final String typeName;

        /**
         * Constructs a NodeType.
         * This constructor initializes the NodeType with a human-readable name.
         *
         * @param typeName The human-readable name of this NodeType.
         */
        NodeType(String typeName) {
            this.typeName = typeName;
        }

        /**
         * Returns the human-readable name of this NodeType.
         *
         * @return The human-readable name of this NodeType.
         */
        @Override
        public String toString() {
            return this.typeName;
        }
    }

    /**
     * Outputs an error message and terminates the program.
     * This method prints out the provided error message along with the line and position, if they are positive.
     * If line or position are not positive, only the error message is printed.
     * After outputting the error message, the method terminates the program with an exit status of 1.
     *
     * @param lineNumber The line number where the error occurred. If not positive, this is not included in the error message.
     * @param positionInLine The position in the line where the error occurred. If not positive, this is not included in the error message.
     * @param errorMessage The error message to print.
     */
    static void displayErrorAndExit(int lineNumber, int positionInLine, String errorMessage) {
        if (lineNumber > 0 && positionInLine > 0) {
            System.out.printf("%s in line %d, pos %d\n", errorMessage, lineNumber, positionInLine);
        } else {
            System.out.println(errorMessage);
        }
        System.exit(1);
    }

    /**
     * Constructs a new Parser object.
     * This constructor initializes the parser with a list of tokens to be parsed.
     * It also sets the initial token to null and the initial position to zero,
     * preparing the parser for the start of the token sequence.
     *
     * @param source A List of Token objects to be parsed by this Parser.
     */
    Parser(List<Token> source) {
        this.source = source;
        this.token = null;
        this.position = 0;
    }

    /**
     * Retrieves the next token from the source list.
     * This method updates the current token to be the next one in the source list and
     * increments the position counter to point to the following token. The updated current token is then returned.
     *
     * @return The next Token from the source list.
     */
    Token getNextToken() {
        this.token = this.source.get(this.position++);
        return this.token;
    }

    /**
     * Parses an expression from the source code and constructs the corresponding node in the abstract syntax tree (AST).
     * This method implements the precedence climbing method for parsing expressions with arbitrary precedence levels.
     *
     * @param precedence The minimal precedence level of operators that this method call should handle.
     * @return The root node of the AST corresponding to the expression parsed.
     */
    Node expr(int precedence) {
        Node expressionNode;

        if (this.token.tokenType == TokenType.LeftParen) {
            expressionNode = parseParenthesizedExpression();
        } else if (isTokenUnaryOperator()) {
            expressionNode = parseUnaryOperation();
        } else if (this.token.tokenType == TokenType.Identifier || this.token.tokenType == TokenType.Integer) {
            expressionNode = parseLeafNode();
        } else {
            displayErrorAndExit(this.token.columnNumber, this.token.columnNumber, this.token.tokenValue);
            return null;
        }

        while (this.token.tokenType.isBinaryOperator() && this.token.tokenType.getPrecedenceValue() >= precedence) {
            expressionNode = parseBinaryOperation(expressionNode);
        }

        return expressionNode;
    }

    /**
     * Checks if the current token represents a unary operator.
     *
     * @return True if the token is a unary operator, false otherwise.
     */
    private boolean isTokenUnaryOperator() {
        return this.token.tokenType == TokenType.Op_subtract || this.token.tokenType == TokenType.Op_add || this.token.tokenType == TokenType.Op_not;
    }

    /**
     * Parses a unary operation from the source code and constructs the corresponding node in the abstract syntax tree (AST).
     *
     * @return The root node of the AST corresponding to the unary operation parsed.
     */
    private Node parseUnaryOperation() {
        TokenType operator = this.token.tokenType == TokenType.Op_subtract ? TokenType.Op_negate : this.token.tokenType;
        getNextToken();
        Node operand = expr(operator.getPrecedenceValue());

        return operator == TokenType.Op_negate
                ? Node.makeNode(NodeType.Negate, operand)
                : Node.makeNode(NodeType.Not, operand);
    }

    /**
     * Parses a leaf node from the source code (either an identifier or an integer)
     * and constructs the corresponding node in the abstract syntax tree (AST).
     *
     * @return The leaf node corresponding to the identifier or integer parsed.
     */
    private Node parseLeafNode() {
        NodeType nodeType = this.token.tokenType == TokenType.Identifier ? NodeType.Identifier : NodeType.Integer;
        Node leafNode = Node.makeLeaf(nodeType, this.token.tokenValue);
        getNextToken();

        return leafNode;
    }

    /**
     * Parses a binary operation from the source code and constructs the corresponding node in the abstract syntax tree (AST).
     *
     * @param leftOperand The node corresponding to the left operand of the binary operation.
     * @return The root node of the AST corresponding to the binary operation parsed.
     */
    private Node parseBinaryOperation(Node leftOperand) {
        TokenType operator = this.token.tokenType;
        getNextToken();
        int operatorPrecedence = operator.isRightAssociative() ? operator.getPrecedenceValue() + 1 : operator.getPrecedenceValue();

        Node rightOperand = expr(operatorPrecedence);

        return Node.makeNode(operator.correspondingNodeType, leftOperand, rightOperand);
    }

    /**
     * Parses a parenthesized expression from the current position in the token stream.
     * This method expects a left parenthesis token, followed by an expression,
     * and then a right parenthesis token.
     *
     * @return A Node representing the parsed expression.
     * @throws RuntimeException If the current token stream does not match this pattern.
     */
    Node parseParenthesizedExpression() {
        expectTokenType("paren_expr: Missing left parenthesis", TokenType.LeftParen);
        Node expressionNode = expr(0);
        expectTokenType("paren_expr: Missing right parenthesis", TokenType.RightParen);
        return expressionNode;
    }

    /**
     * Checks whether the current token type matches the expected token type.
     * If the current token type matches, it proceeds to get the next token and then returns.
     * If the current token type does not match, it throws an error containing
     * the expected token type, the actual token type, and the provided error message.
     *
     * @param errorMessage The message to display if the token types don't match.
     * @param expectedTokenType The expected token type.
     * @throws RuntimeException If the current token type does not match the expected token type.
     */
    void expectTokenType(String errorMessage, TokenType expectedTokenType) {
        if (this.token.tokenType == expectedTokenType) {
            getNextToken();
            return;
        }
        throw new RuntimeException(errorMessage + ": Expecting '" + expectedTokenType +
                "', found: '" + this.token.tokenType + "' at line " + this.token.lineNumber +
                ", position " + this.token.columnNumber);
    }

    /**
     * Parses a single statement in the source code and constructs the corresponding node in the abstract syntax tree (AST).
     * The method is designed to handle different types of statements as defined in the language grammar.
     * It's a recursive function, where each recursive call handles one component of a statement,
     * such as an if-clause, a while loop, an assignment operation, etc.
     *
     * @return The root node of the AST corresponding to the statement parsed.
     */
    Node parseStatement() {
        Node statementNode = null;

        switch (this.token.tokenType) {
            case Keyword_if -> statementNode = parseIfStatement();
            case Keyword_putc -> statementNode = parsePutcStatement();
            case Keyword_print -> statementNode = parsePrintStatement();
            case Semicolon -> getNextToken();
            case Identifier -> statementNode = parseAssignmentStatement();
            case Keyword_while -> statementNode = parseWhileStatement();
            case LeftBrace -> statementNode = parseBraceStatement();
            case End_of_input -> {
                assert true;
            }
            default -> displayErrorAndExit(this.token.lineNumber, this.token.columnNumber,
                    "Expected start of statement, instead found: " + this.token);
        }

        return statementNode;
    }

    /**
     * Parses an 'if' statement and constructs the corresponding node in the AST.
     *
     * @return The root node of the AST corresponding to the 'if' statement parsed.
     */
    private Node parseIfStatement() {
        getNextToken();
        Node expressionNode = parseParenthesizedExpression();
        Node ifStatement = parseStatement();
        Node elseStatement = null;
        if (this.token.tokenType == TokenType.Keyword_else) {
            getNextToken();
            elseStatement = parseStatement();
        }
        return Node.makeNode(NodeType.If, expressionNode, Node.makeNode(NodeType.If, ifStatement, elseStatement));
    }

    /**
     * Parses a 'putc' statement and constructs the corresponding node in the AST.
     *
     * @return The root node of the AST corresponding to the 'putc' statement parsed.
     */
    private Node parsePutcStatement() {
        getNextToken();
        Node expressionNode = parseParenthesizedExpression();
        expectTokenType(TokenType.Keyword_putc.name(), TokenType.Semicolon);
        return Node.makeNode(NodeType.PrintCharacter, expressionNode);
    }

    /**
     * Parses a 'print' statement and constructs the corresponding node in the AST.
     *
     * @return The root node of the AST corresponding to the 'print' statement parsed.
     */
    private Node parsePrintStatement() {
        getNextToken();
        expectTokenType(TokenType.Keyword_print.name(), TokenType.LeftParen);
        Node statementNode = null;
        while (true) {
            Node expressionNode;
            if (this.token.tokenType == TokenType.String) {
                expressionNode = Node.makeNode(NodeType.PrintString, Node.makeLeaf(NodeType.String, this.token.tokenValue));
                getNextToken();
            } else {
                expressionNode = Node.makeNode(NodeType.PrintInteger, expr(0));
            }
            statementNode = Node.makeNode(NodeType.Sequence, statementNode, expressionNode);
            if (this.token.tokenType != TokenType.Comma) {
                break;
            }
            getNextToken();
        }
        expectTokenType(TokenType.Keyword_print.name(), TokenType.RightParen);
        expectTokenType(TokenType.Keyword_print.name(), TokenType.Semicolon);
        return statementNode;
    }

    /**
     * Parses an assignment statement and constructs the corresponding node in the AST.
     *
     * @return The root node of the AST corresponding to the assignment statement parsed.
     */
    private Node parseAssignmentStatement() {
        Node identifierNode = Node.makeLeaf(NodeType.Identifier, this.token.tokenValue);
        getNextToken();
        expectTokenType(TokenType.Op_assign.name(), TokenType.Op_assign);
        Node expressionNode = expr(0);
        expectTokenType(TokenType.Op_assign.name(), TokenType.Semicolon);
        return Node.makeNode(NodeType.Assign, identifierNode, expressionNode);
    }

    /**
     * Parses a 'while' statement and constructs the corresponding node in the AST.
     *
     * @return The root node of the AST corresponding to the 'while' statement parsed.
     */
    private Node parseWhileStatement() {
        getNextToken();
        Node expressionNode = parseParenthesizedExpression();
        Node whileStatement = parseStatement();
        return Node.makeNode(NodeType.While, expressionNode, whileStatement);
    }

    /**
     * Parses a statement within braces (i.e., a block of statements) and constructs the corresponding nodes in the AST.
     *
     * @return The root node of the AST corresponding to the block of statements parsed.
     */
    private Node parseBraceStatement() {
        getNextToken();
        Node statementNode = null;
        while (this.token.tokenType != TokenType.RightBrace && this.token.tokenType != TokenType.End_of_input) {
            statementNode = Node.makeNode(NodeType.Sequence, statementNode, parseStatement());
        }
        expectTokenType(TokenType.LeftBrace.name(), TokenType.RightBrace);
        return statementNode;
    }

    /**
     * Parses the sequence of tokens currently loaded in the parser.
     * This method starts by calling getNextToken() to load the first token,
     * and then enters a loop where it continues to parse tokens into statements until
     * it encounters the 'End_of_input' token.
     * It uses these parsed statements to construct an Abstract Syntax Tree (AST),
     * where each newly parsed statement is added to the right side of the current root
     * and the old root is attached to the left side, effectively creating a reversed linked list.
     *
     * @return The root Node of the constructed AST, or null if no tokens were parsed.
     */
    Node parse() {
        Node rootNode = null;
        getNextToken();
        while (this.token.tokenType != TokenType.End_of_input) {
            rootNode = Node.makeNode(NodeType.Sequence, rootNode, parseStatement());
        }
        return rootNode;
    }

    /**
     * Traverses the given Abstract Syntax Tree (AST), starting from the given Node.
     * The method prints out the NodeType of each Node it visits,
     * along with the Node's value if its NodeType is nd_Ident, nd_Integer, or nd_String.
     * The traversing is done in pre-order (root, left, right).
     * The information is both printed to the standard output and appended to the given StringBuilder.
     *
     * @param rootNode The root Node of the AST to be printed
     * @param stringBuilder The StringBuilder to append the output to
     * @return A String representation of the traversed AST
     */
    String printAST(Node rootNode, StringBuilder stringBuilder) {
        if (rootNode == null) {
            stringBuilder.append(";").append("\n");
            System.out.println(";");
        } else {
            stringBuilder.append(rootNode.nodeType);
            System.out.printf("%-14s", rootNode.nodeType);

            if (rootNode.nodeType == NodeType.Identifier || rootNode.nodeType == NodeType.Integer || rootNode.nodeType == NodeType.String) {
                stringBuilder.append(" ").append(rootNode.nodeValue).append("\n");
                System.out.println(" " + rootNode.nodeValue);
            } else {
                stringBuilder.append("\n");
                System.out.println();
                printAST(rootNode.leftChild, stringBuilder);
                printAST(rootNode.rightChild, stringBuilder);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Writes the given result to a file named "hello.par" located in the src/main/resources directory.
     * If the file does not exist, it is created. If it does exist, it is overwritten.
     * After successful writing, a confirmation message is printed to standard output.
     * If any IOException occurs during the process, it is encapsulated in a RuntimeException and thrown.
     *
     * @param result the String content to be written to the file
     * @throws RuntimeException if an IOException occurred during file writing process
     */
    static void outputToFile(String result) {
        try {
            FileWriter myWriter = new FileWriter("src/main/resources/count.par");
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The entry point of the application.
     * This method starts by checking a condition (which currently is always true).
     * Then, it reads token definitions from a file named "hello.lex" in the src/main/resources directory.
     * It uses these tokens to initialize a Parser, and then parses the tokens into an Abstract Syntax Tree (AST).
     * The AST is then printed to standard output and also saved to a file.
     * If any exceptions occur during the process, an error message is printed to standard output.
     *
     * @param args The command-line arguments. Currently unused.
     */
    public static void main(String[] args) {
        try {
            StringBuilder tokenValue;
            String tokenName;
            String parseResult;
            StringBuilder stringBuilder = new StringBuilder();
            int lineNumber;
            int position;
            boolean tokenFound;
            List<Token> tokenList = new ArrayList<>();
            Map<String, TokenType> stringToTokenTypes = new HashMap<>();
            initializeTokenTypes(stringToTokenTypes);
            Scanner scanner = new Scanner(new File("src/main/resources/count.lex"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                StringTokenizer stringTokenizer = new StringTokenizer(line);
                lineNumber = Integer.parseInt(stringTokenizer.nextToken());
                position = Integer.parseInt(stringTokenizer.nextToken());
                tokenName = stringTokenizer.nextToken();
                tokenValue = new StringBuilder();
                while (stringTokenizer.hasMoreTokens()) {
                    tokenValue.append(stringTokenizer.nextToken()).append(" ");
                }
                tokenFound = false;
                if (stringToTokenTypes.containsKey(tokenName)) {
                    tokenFound = true;
                    tokenList.add(new Token(stringToTokenTypes.get(tokenName), tokenValue.toString(), lineNumber, position));
                }
                if (!tokenFound) {
                    throw new Exception("Token not found: '" + tokenName + "'");
                }
            }
            Parser parser = new Parser(tokenList);
            parseResult = parser.printAST(parser.parse(), stringBuilder);
            outputToFile(parseResult);
        } catch (Exception e) {
            displayErrorAndExit(-1, -1, "Exception: " + e.getMessage());
        }
    }

    /**
     * Initializes the map that translates strings into TokenType enums.
     * This method maps a predefined set of string keys to corresponding TokenType values.
     * This map is used during the token parsing process to convert string representations
     * of token types from the input file into the corresponding TokenType enum values.
     *
     * @param str_to_tokens The map to initialize with TokenType mappings.
     *                           Key is the string representation of the TokenType.
     *                           Value is the corresponding TokenType.
     */
    private static void initializeTokenTypes(Map<String, TokenType> str_to_tokens) {
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
    }
}