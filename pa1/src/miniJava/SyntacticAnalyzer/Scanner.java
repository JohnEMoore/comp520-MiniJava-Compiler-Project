package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;

	private boolean EOT = False;
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this._currentText = new StringBuilder();
		
		nextChar();
	}
	
	public Token scan() {
		// TODO: This function should check the current char to determine what the token could be.

		// TODO: Consider what happens if the current char is whitespace
		
		// TODO: Consider what happens if there is a comment (// or /* */)
		
		// TODO: What happens if there are no more tokens?
		
		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.

		if(EOT == True){

			return null;
		}

		if(_currentChar == "\4"){
			EOT = True;
			return makeToken(TokenType.EOT);
		}

		while (Character.isWhitespace(_currentChar)){
			skipIt();
		}


		if (_currentChar == '/'){
			takeIt();
			if (_currentChar == '/'){
				while(_currentChar != '\n'){
					skipIt();
				}
				return scan();
			}
			if(_currentChar == '*'){
				while(true){
					if(_currentChar == '*'){
						skipIt();
						if(_currentChar == '/'){
							skipIt();
							return scan()
						}
					}
				}
			}

			return makeToken(TokenType.OPERATOR, _currentText)
		}

		if (Character.isDigit(_currentChar)) {
			takeIt();
			while(_currentChar.isDigit()){
				takeIt();
			}
			return makeToken(TokenType.INTLITERAL, _currentText);
		}
		if(Character.isAlphabetic()){
			takeIt();
			while(_currentChar.isLetterOrDigit() || _currentChar =='_'){
				takeIt();
				}
			TokenType tokType = null;
			switch (_currentText){
				case 'static':
					tokType = TokenType.ACCESS;
				case 'class':
					tokType = TokenType.CLASS;
				case 'else':
					tokType = TokenType.ELSE;
				case 'false':
					tokType = TokenType.FALSE;
				case 'true':
					tokType = TokenType.True;
				case 'if':
					tokType = TokenType.IF;

				case 'new':
					tokType = TokenType.NEW;

				case 'this':
					tokType = TokenType.THIS;
				case 'int':
					tokType = TokenType.INTEGER;
				case 'boolean':
					tokType = TokenType.BOOLEAN;
				case 'return':
					tokType = TokenType.RETURN;
				case 'public':
				case 'private':
					tokType = TokenType.VISIBILTY;
				case 'void':
					tokType = TokenType.VOID;
				case 'while':
					tokType = TokenType.WHILE;
				default:
					tokType = TokenType.ID;
				return makeToken(tokType, _currentText);
			}
		}

		else {
			switch (_currentText) {

				case ',':
					takeIt();
					return makeToken(TokenType.COMMA);
				case '[':
					takeIt();
					return makeToken(TokenType.LBLOCK)
				case '{'
					takeIt();
					return makeToken(TokenType.LCURLY);
				case '(':
					takeIt();
					return makeToken(TokenType.LPAREN);
				case '!':
					takeIt();
					return makeToken(TokenType.OPERATOR);
				case '+':
					takeIt();
					return makeToken(TokenType.OPERATOR);
				case '-':
					takeIt();
					return makeToken(TokenType.OPERATOR);
				case '*':
					takeIt();
					return makeToken(TokenType.OPERATOR);
				case '>':
				case '<':
				case '!':
					takeIt();
					if(_currentChar == '='){
						takeIt();
						return makeToken(TokenType.OPERATOR);
					}
					return makeToken(TokenType.OPERATOR);
				case '=':
					if(_currentChar == '='){
						takeIt();
						return makeToken(TokenType.OPERATOR);
					}
					return  makeToken(TokenType.EQUALS);
				case '&':
					takeIt();
					if(_currentChar == '&'){
						takeIt();
						return makeToken(TokenType.OPERATOR);
					}
					throw Error;
				case '|':
					takeIt();
					if(_currentChar == '|'){
						takeIt();
						return makeToken(TokenType.OPERATOR);
					}
					throw Error;
				case ']':
					takeIt();
					return makeToken(TokenType.RBLOCK);
				case '}':
					takeIt();
					return makeToken(TokenType.RCURLY);
				case ')':
					takeIt();
					return makeToken(TokenType.RPAREN);
				case ';':
					takeIt();
					return makeToken(TokenType.SEMICOLON);

			}
		}





		return null;
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			
			// TODO: What happens if c == -1?

			if (c == -1){
				//stop getting input
				// should I check if there is a semicolon
				_currentChar = "\4";
			}
			// TODO: What happens if c is not a regular ASCII character?
			if (c >= 127){
				//non ascii throw error
				//throw new UnmappableCharacterException();
				_errors.reportError("Unmappable Character Exception")
			}

		} catch( IOException e ) {
			// TODO: Report an error here
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in

		Token myToken = new Token(toktype, _currentText.toString());
		_currentText = new StringBuilder();
		return myToken;
	}
}
