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
		} catch( Throwable e ) {
			_errors.reportError("Invalid token");
		}
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
		if(_currentToken.getTokenType() == TokenType.ACCESS) {
			accept(TokenType.ACCESS);
		}
		if(_currentToken.getTokenType() == TokenType.VOID) {
			//in a method
			accept(TokenType.VOID);
			accept(TokenType.ID);
			accept(TokenType.LPAREN);
			if (_currentToken.getTokenType() != TokenType.RPAREN) {
				// at least one parameter
				parseParameterList();
			}
			accept(TokenType.RPAREN);
			accept(TokenType.LCURLY);
			while (_currentToken.getTokenType() != TokenType.RCURLY) {
				parseStatement();
			}
			accept(TokenType.RCURLY);


		}
		else if(_currentToken.getTokenType() == TokenType.INTEGER || _currentToken.getTokenType() == TokenType.BOOLEAN || _currentToken.getTokenType() == TokenType.ID){
			// could be method or field
			parseType();
			accept(TokenType.ID);
			switch (_currentToken.getTokenType()){
				case  SEMICOLON:
					accept(TokenType.SEMICOLON);
					break;
				case LPAREN:
					//method
					accept(TokenType.LPAREN);
					if (_currentToken.getTokenType() != TokenType.RPAREN) {
						// at least one parameter
						parseParameterList();
					}
					accept(TokenType.RPAREN);
					accept(TokenType.LCURLY);
					while (_currentToken.getTokenType() != TokenType.RCURLY) {
						parseStatement();
					}
					accept(TokenType.RCURLY);
					break;

				default:
					_errors.reportError("Expected Type for declaration");

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
				break;
			case ID:
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.LBLOCK){
					accept(TokenType.LBLOCK);
					accept(TokenType.RBLOCK);
				}
				break;
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
				break;
			case RETURN:
				accept(TokenType.RETURN);
				if(_currentToken.getTokenType() != TokenType.SEMICOLON){
					parseExpression();
				}
				accept(TokenType.SEMICOLON);
				break;
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
				break;
			case WHILE:
				accept(TokenType.WHILE);
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
				parseStatement();
				break;
			case THIS:
				//REFERENCE generation
				parseReference();
				switch (_currentToken.getTokenType()){
					case EQUALS:
						accept(TokenType.EQUALS);
						parseExpression();
						accept(TokenType.SEMICOLON);
						break;
					case LBLOCK:
						accept(TokenType.LBLOCK);
						parseExpression();
						accept(TokenType.RBLOCK);
						accept(TokenType.EQUALS);
						parseExpression();
						accept(TokenType.SEMICOLON);
						break;
					case LPAREN:
						accept(TokenType.LPAREN);
						parseArgumentList();
						accept(TokenType.RPAREN);
						accept(TokenType.SEMICOLON);
						break;
				}
				break;
			case INTEGER:
			case BOOLEAN:
				parseType();
				accept(TokenType.ID);
				accept(TokenType.EQUALS);
				parseExpression();
				accept(TokenType.SEMICOLON);
				break;
			default:
				// can be type or reference
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.PERIOD) {
					parseReference();
					switch (_currentToken.getTokenType()) {
						case EQUALS:
							accept(TokenType.EQUALS);
							parseExpression();
							accept(TokenType.SEMICOLON);
							break;
						case LBLOCK:
							accept(TokenType.LBLOCK);
							parseExpression();
							accept(TokenType.RBLOCK);
							accept(TokenType.EQUALS);
							parseExpression();
							accept(TokenType.SEMICOLON);
							break;
						case LPAREN:
							accept(TokenType.LPAREN);
							parseArgumentList();
							accept(TokenType.RPAREN);
							accept(TokenType.SEMICOLON);
							break;
					}
				} else if (_currentToken.getTokenType() == TokenType.LPAREN) {
					accept(TokenType.LPAREN);
					parseArgumentList();
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOLON);

				} else if (_currentToken.getTokenType() == TokenType.EQUALS){

					accept(TokenType.EQUALS);
					parseExpression();

					}
				else if (_currentToken.getTokenType() == TokenType.ID) {
					//first id was type this is the id Type id = Expression;
					accept(TokenType.ID);
					accept(TokenType.EQUALS);
					parseExpression();
					accept(TokenType.SEMICOLON);
				} else if (_currentToken.getTokenType() == TokenType.LBLOCK) {
					accept(TokenType.LBLOCK);
					if(_currentToken.getTokenType() == TokenType.RBLOCK){
						//TYPE
						accept(TokenType.RBLOCK);
						accept(TokenType.ID);
						accept(TokenType.EQUALS);
						parseExpression();
						accept(TokenType.SEMICOLON);
					}
					else {
						parseExpression();
						accept(TokenType.RBLOCK);
					}

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
				break;
			case TRUE:
				accept(TokenType.TRUE);
				break;
			case FALSE:
				accept(TokenType.FALSE);
				break;
			case LPAREN:
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
				break;
			case OPERATOR:
				if(_currentToken.getTokenText() == "!" || _currentToken.getTokenText() == "-") {
					accept(TokenType.OPERATOR);
					parseExpression();
					break;
				}
				else{
					_errors.reportError("Expected unary operator");
				}
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
				break;
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
				break;


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
		_errors.reportError(String.format("Expected %s, but got %s", expectedType.name(), _currentToken.getTokenText()));
		throw new SyntaxError();
	}
}
