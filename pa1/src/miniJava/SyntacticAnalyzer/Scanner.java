package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	
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

		while _currentChar != null && _currentChar == ' '{
			skipIt();
		}
		/* Should I store previous characters to see if there is a comment that happens? */
		// if i do I can skip while in commentmode and once there is an end of the block or it stops I continue as normal

		_currentText == new StringBuilder();
		TokenType tokType = null;
		String lexType = null;

		while(_currentChar != null && _currentChar != ' '){
			//decide what token this is
			if lexType == null{

				if (30 <= _currentChar &&  _currentChar <= 39)
					lexType = "number";
				if (65 <= _currentChar && _currentChar<= 90 || 97 <= _currentChar && _currentChar <= 122)
					lexType = "alphanumeric"; // some way to classify token before it is complete

			}
			// if lexType is decided, see if the new character breaks the pattern (such as a letter after a number and break

			takeIt();
		}

		if (_currentText.length() != 0){
			return makeToken(tokType);
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

			if c == -1{
				//stop getting input
				// should I check if there is a semicolon
				_currentChar = null;
			}

			// TODO: What happens if c is not a regular ASCII character?

			// /doesn't that just throw an error that is getting caught anyway?
			
		} catch( IOException e ) {
			// TODO: Report an error here
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in 
		return new Token(toktype, _currentText)
	}
}
