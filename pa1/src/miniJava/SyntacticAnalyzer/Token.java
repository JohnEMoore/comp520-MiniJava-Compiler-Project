package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;

	private SourcePosition _pos;
	
	public Token(TokenType type, String text,SourcePosition pos) {
		this._type = type;
		this._text = text;
		this._pos = pos;
	}
	
	public TokenType getTokenType() {
		return _type;
	}
	
	public String getTokenText() {
		return _text;
	}

	public SourcePosition getTokenPosition(){
		return _pos;
	}
}
