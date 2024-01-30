package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public void parse() {
		try {
			// The first thing we need to parse is the Program symbol
			parseProgram();
		} catch( SyntaxError e ) { }
	}
	
	// Program ::= (ClassDeclaration)* eot
	private void parseProgram() throws SyntaxError {
		// TODO: Keep parsing class declarations until eot
		if(_currentToken.getTokenType() != TokenType.EOT) {
			parseClassDeclaration();
		}

		accept((TokenType.EOT));

	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private void parseClassDeclaration() throws SyntaxError {
		// TODO: Take in a "class" token (check by the TokenType)
		//  What should be done if the first token isn't "class"?
		accept(TokenType.CLASS);

		// TODO: Take in an identifier token
		accept(TokenType.ID);
		// TODO: Take in a {
		accept(TokenType.LCURLY);
		// TODO: Parse either a FieldDeclaration or MethodDeclaration
		if(_currentToken.getTokenType() == TokenType.VISIBILITY) {
			accept(TokenType.VISIBILITY);
		}
		if(_currentToken.getTokenType() == TokenType.VISIBILITY) {
			accept(TokenType.ACCESS);
		}
		if(_currentToken.getTokenType() == TokenType.VOID) {
			//in a method
			accept(TokenType.VOID);
			accept(TokenType.ID);
			accept(TokenType.LPAREN);
			while (_currentToken.getTokenType() != TokenType.RPAREN) {
				// at least one parameter
				parseParameterList();
			}
			accept(TokenType.LCURLY);
			while (_currentToken.getTokenType() != TokenType.RCURLY) {
				parseStatement();
			}
			accept(TokenType.RCURLY);
		}
		else{
			// could be method or field
			parseType();
			accept(TokenType.ID);
			switch (_currentToken.getTokenType()){
				case  SEMICOLON:
					accept(TokenType.SEMICOLON);
				case LPAREN:
					//method
					accept(TokenType.LPAREN);
				default:
					//error

			}


			}


		// TODO: Take in a }
		accept(TokenType.RCURLY);

	}

	private void parseType() throws SyntaxError {
		switch (_currentToken.getTokenType()){
			case INTEGER:
				accept(TokenType.INTEGER);
				if(_currentToken.getTokenType() == TokenType.LBLOCK){
					accept(TokenType.LBLOCK);
					accept(TokenType.RBLOCK);
				}
			case ID:
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.LBLOCK){
					accept(TokenType.LBLOCK);
					accept(TokenType.RBLOCK);
				}
			case BOOLEAN:
				accept(TokenType.BOOLEAN);


		}
	}

	private void parseParameterList() throws SyntaxError {
		parseType();
		accept(TokenType.ID);
		while (_currentToken.getTokenType() == TokenType.COMMA){
			parseType();
			accept(TokenType.ID);
		}
	}

	private void parseStatement(){
		switch (_currentToken.getTokenType()){
			case LCURLY:
				accept(TokenType.LCURLY);
				while (_currentToken.getTokenType() != TokenType.RCURLY) {
					parseStatement();
				}
				accept(TokenType.RCURLY);
			case RETURN:
				accept(TokenType.RETURN);
				if(_currentToken.getTokenType() != TokenType.SEMICOLON){
					parseExpression();
				}
				accept(TokenType.SEMICOLON);
			case IF:
				accept(TokenType.IF);
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
				parseStatement();
				if(_currentToken.getTokenType() == TokenType.ELSE){
					accept(TokenType.ELSE);
					parseStatement();
				}
			case WHILE:
				accept(TokenType.WHILE);
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
				parseStatement();
			case ID:
			case THIS:
				//REFERENCE generation
				parseReference();
				switch (_currentToken.getTokenType()){
					case EQUALS:
						accept(TokenType.EQUALS);
						parseExpression();
						accept(TokenType.SEMICOLON);
					case LBLOCK:
						accept(TokenType.LBLOCK);
						parseExpression();
						accept(TokenType.RBLOCK);
						accept(TokenType.EQUALS);
						parseExpression();
						accept(TokenType.SEMICOLON);
					case LPAREN:
						accept(TokenType.LPAREN);
						parseArgumentList();
						accept(TokenType.RPAREN);
						accept(TokenType.SEMICOLON);
				}

		}
	}

	private void parseArgumentList(){
		parseExpression();
		while(_currentToken.getTokenType() == TokenType.COMMA){
			accept(TokenType.COMMA);
			parseExpression();
		}
	}

	private void parseReference(){
		if (_currentToken.getTokenType() == TokenType.ID){
			accept(TokenType.ID);
		}
		else if(_currentToken.getTokenType() == TokenType.THIS){
			accept(TokenType.THIS);
		}
		while(_currentToken.getTokenType() == TokenType.PERIOD){
			accept(TokenType.PERIOD);
			accept(TokenType.ID);
		}
	}
	private void parseExpression(){
		switch (_currentToken.getTokenType()){
			case INTLITERAL:
				accept(TokenType.INTLITERAL);
			case TRUE:
				accept(TokenType.TRUE);
			case FALSE:
				accept(TokenType.FALSE);
			case LPAREN:
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
			case OPERATOR:
				accept(TokenType.OPERATOR);
				parseExpression();
			case NEW:
				accept(TokenType.NEW);
				switch (_currentToken.getTokenType()){
					case ID:
						accept(TokenType.ID);
						if (_currentToken.getTokenType() == TokenType.LPAREN){
							accept(TokenType.LPAREN);
							accept(TokenType.RPAREN);
						}
						else/* (_currentToken.getTokenType() == TokenType.LBLOCK) */{
							accept(TokenType.LBLOCK);
							parseExpression();
							accept(TokenType.RBLOCK);
						}
					case INTEGER:
						accept(TokenType.INTEGER);
						accept(TokenType.LBLOCK);
						parseExpression();
						accept(TokenType.RBLOCK);
				}
			case ID:
			case THIS:
				parseReference();
				if (_currentToken.getTokenType() == TokenType.LPAREN){
					accept(TokenType.LPAREN);
					if (_currentToken.getTokenType() != TokenType.RPAREN){
						parseArgumentList();
					}
					accept(TokenType.RPAREN);
				} else if (_currentToken.getTokenType() == TokenType.LBLOCK) {
					accept(TokenType.LBLOCK);
					parseExpression();
					accept(TokenType.RBLOCK);
				}


		}
		if (_currentToken.getTokenType() == TokenType.OPERATOR){
			accept(TokenType.OPERATOR);
			parseExpression();
		}
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType ) {
			_currentToken = _scanner.scan();
			return;
		}
		
		// TODO: Report an error here.
		//  "Expected token X, but got Y"
		//throw new SyntaxError();
		_errors.reportError("Expected %s, but got %s", expectedType.toString(), _currentToken.toString());
	}
}
