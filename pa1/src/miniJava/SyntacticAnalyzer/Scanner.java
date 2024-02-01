package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;

	private StringBuilder _debug = new StringBuilder();

	private boolean EOT = false;
	public int line = 1;
	public int column = 0;
	
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

		if(EOT){

			return null;
		}



		while (Character.isWhitespace(_currentChar)){

			skipIt();
		}


		if (_currentChar == '/'){
			takeIt();
			if (_currentChar == '/'){
				skipIt();
				_currentText = new StringBuilder();
				while(_currentChar != '\n'){
					if (_currentChar == '\u0004'){
						takeIt();
						return makeToken(TokenType.EOT);
					}
					skipIt();
				}

				return scan();
			}
			if(_currentChar == '*'){
				_currentText = new StringBuilder();
				skipIt();
				while(true){
					if(_currentChar == '*'){
						skipIt();
						if(_currentChar == '/'){
							skipIt();
							return scan();
						}
					}
					else {
						skipIt();
					}
				}
			}

			return makeToken(TokenType.OPERATOR);
		}

		if(_currentChar == '\u0004'){
			EOT = true;
			return makeToken(TokenType.EOT);
		}

		if (Character.isDigit(_currentChar)) {
			takeIt();
			while(Character.isDigit(_currentChar)){
				takeIt();
			}
			return makeToken(TokenType.INTLITERAL);
		}
		if(Character.isAlphabetic(_currentChar)){
			takeIt();
			while(Character.isLetterOrDigit(_currentChar) || _currentChar =='_'){
				takeIt();
				}
			TokenType tokType = null;
			String cur = _currentText.toString();
			switch (cur){
				case "static":
					tokType = TokenType.ACCESS;
					break;
				case "class":
					tokType = TokenType.CLASS;
					break;
				case "else":
					tokType = TokenType.ELSE;
					break;
				case "false":
					tokType = TokenType.FALSE;
					break;
				case "true":
					tokType = TokenType.TRUE;
					break;
				case "if":
					tokType = TokenType.IF;
					break;
				case "new":
					tokType = TokenType.NEW;
					break;
				case "this":
					tokType = TokenType.THIS;
					break;
				case "int":
					tokType = TokenType.INTEGER;
					break;
				case "boolean":
					tokType = TokenType.BOOLEAN;
					break;
				case "return":
					tokType = TokenType.RETURN;
					break;
				case "public":
				case "private":
					tokType = TokenType.VISIBILITY;
					break;
				case "void":
					tokType = TokenType.VOID;
					break;
				case "while":
					tokType = TokenType.WHILE;
					break;
				default:
					tokType = TokenType.ID;

			}
			return makeToken(tokType);
		}

		else {
			switch (_currentChar) {

				case ',':
					takeIt();
					return makeToken(TokenType.COMMA);
				case '[':
					takeIt();
					return makeToken(TokenType.LBLOCK);
				case '{':
					takeIt();
					return makeToken(TokenType.LCURLY);
				case '(':
					takeIt();
					return makeToken(TokenType.LPAREN);
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
					takeIt();
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
					_currentText = new StringBuilder();
					_currentText.append("Invalid Token Type");
					return makeToken(TokenType.INVALIDTOKEN);
				case '|':
					takeIt();
					if(_currentChar == '|'){
						takeIt();
						return makeToken(TokenType.OPERATOR);
					}
					_currentText = new StringBuilder();
					_currentText.append("Invalid Token Type");
					return makeToken(TokenType.INVALIDTOKEN);
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
				case '.':
					takeIt();
					return makeToken(TokenType.PERIOD);
				default:
					_currentText.append("Invalid Token Type");
					return makeToken(TokenType.INVALIDTOKEN);


			}
		}

	}
	
	private void takeIt() {
		this.column += 1;
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		if (_currentChar == '\n'){
			this.line += 1;
			this.column = 0;
		}
		else{
			this.column += 1;
		}
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			_debug.append(_currentChar);
			
			// TODO: What happens if c == -1?

			if (c == -1){
				_currentChar = '\4';
			}
			// TODO: What happens if c is not a regular ASCII character?
			if (c >= 127){
				//non ascii throw error
				//throw new UnmappableCharacterException();
				_errors.reportError("Unmappable Character Exception");
				throw new Error();
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
