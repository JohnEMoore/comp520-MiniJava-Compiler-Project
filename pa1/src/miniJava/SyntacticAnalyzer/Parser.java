package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	private Token _prevToken;

	private int precedence_level = 0;

	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public Package parse() {
		Package retting = new Package(new ClassDeclList(), this._currentToken.getTokenPosition());

		// The first thing we need to parse is the Program symbol
		ClassDeclList progClasses = parseProgram();
		return  new Package(progClasses , _scanner.getPos());




	}
	
	// Program ::= (ClassDeclaration)* eot
	private ClassDeclList parseProgram() throws SyntaxError {
		// TODO: Keep parsing class declarations until eot
		ClassDeclList ret = new ClassDeclList();
		while(_currentToken.getTokenType() != TokenType.EOT) {
			ret.add(parseClassDeclaration());
		}

		accept((TokenType.EOT));
		return ret;

	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();



		// TODO: Take in a "class" token (check by the TokenType)
		//  What should be done if the first token isn't "class"?

		accept(TokenType.CLASS);

		// TODO: Take in an identifier token
		String className = _currentToken.getTokenText();
		accept(TokenType.ID);
		// TODO: Take in a {
		accept(TokenType.LCURLY);
		// TODO: Parse either a FieldDeclaration or MethodDeclaration
		while(_currentToken.getTokenType() != TokenType.RCURLY) {
			Boolean isStatic = false;
			Boolean isPrivate = false;
			String memberName = "";
			if (_currentToken.getTokenType() == TokenType.VISIBILITY) {
				if(_currentToken.getTokenText().equals("private")){
					isPrivate = true;
				}
				accept(TokenType.VISIBILITY);
			}
			if (_currentToken.getTokenType() == TokenType.ACCESS) {
				if(_currentToken.getTokenText().equals("static")){
					isStatic = true;
				}
				accept(TokenType.ACCESS);
			}
			if (_currentToken.getTokenType() == TokenType.VOID) {
				//in a method
				BaseType TD = new BaseType(TypeKind.VOID, _currentToken.getTokenPosition());
				accept(TokenType.VOID);
				memberName = _currentToken.getTokenText();
				accept(TokenType.ID);
				accept(TokenType.LPAREN);
				ParameterDeclList pl = new ParameterDeclList();
				if (_currentToken.getTokenType() != TokenType.RPAREN) {
					// at least one parameter
					pl = parseParameterList();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.LCURLY);
				StatementList sl = new StatementList();
				while (_currentToken.getTokenType() != TokenType.RCURLY) {
					sl.add(parseStatement());
				}
				accept(TokenType.RCURLY);

				mdl.add(new MethodDecl(new FieldDecl(isPrivate, isStatic, TD, memberName, _prevToken.getTokenPosition()), pl, sl,  _prevToken.getTokenPosition()));


			} else if (_currentToken.getTokenType() == TokenType.INTEGER || _currentToken.getTokenType() == TokenType.BOOLEAN || _currentToken.getTokenType() == TokenType.ID) {
				// could be method or field

				TypeDenoter TD = parseType();
				memberName = _currentToken.getTokenText();
				accept(TokenType.ID);
				switch (_currentToken.getTokenType()) {
					case SEMICOLON:
						accept(TokenType.SEMICOLON);
						fdl.add(new FieldDecl(isPrivate, isStatic, TD, memberName, _prevToken.getTokenPosition()));
						break;
					case LPAREN:
						//method
						accept(TokenType.LPAREN);
						ParameterDeclList pl = new ParameterDeclList();
						if (_currentToken.getTokenType() != TokenType.RPAREN) {
							// at least one parameter
							pl = parseParameterList();
						}
						accept(TokenType.RPAREN);
						accept(TokenType.LCURLY);
						StatementList sl = new StatementList();
						Statement stmne = null;
						while (_currentToken.getTokenType() != TokenType.RCURLY) {
							stmne = parseStatement();
							sl.add(stmne);
						}
						accept(TokenType.RCURLY);
						mdl.add(new MethodDecl(new FieldDecl(isPrivate, isStatic, TD, memberName, _prevToken.getTokenPosition()), pl, sl,  _prevToken.getTokenPosition()));
						break;
					default:
						_errors.reportError("Expected Type for declaration");

				}


			}
			else{
				if(_currentToken.getTokenType() == TokenType.EOT){
					_errors.reportError("Didn't close braces");
				}
				else{
					_errors.reportError("Expected field or method declaration but got " + _currentToken.getTokenType());
				}
				throw new Error();
			}
		}
		ClassDecl classed = new ClassDecl(className, fdl, mdl, _currentToken.getTokenPosition());
		// TODO: Take in a }
		accept(TokenType.RCURLY);
		return classed;

	}

	private TypeDenoter parseType() throws SyntaxError {
		switch (_currentToken.getTokenType()){
			case INTEGER:
				TypeDenoter basedType = new BaseType(TypeKind.INT, this._currentToken.getTokenPosition());
				accept(TokenType.INTEGER);

				if(_currentToken.getTokenType() == TokenType.LBLOCK){
					accept(TokenType.LBLOCK);
					accept(TokenType.RBLOCK);
					return new ArrayType(basedType, this._currentToken.getTokenPosition());
				}
				return basedType;

			case ID:
				TypeDenoter classyType = new ClassType(new Identifier(this._currentToken), this._currentToken.getTokenPosition());
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.LBLOCK){
					accept(TokenType.LBLOCK);
					accept(TokenType.RBLOCK);
					return new ArrayType(classyType, this._currentToken.getTokenPosition());
				}
				return classyType;

			default:
				TypeDenoter boolType = new BaseType(TypeKind.BOOLEAN, this._currentToken.getTokenPosition());
				accept(TokenType.BOOLEAN);
				return boolType;



		}
	}

	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList PDL = new ParameterDeclList();
		TypeDenoter TD = parseType();
		ParameterDecl PD = new ParameterDecl(TD, _currentToken.getTokenText(), _currentToken.getTokenPosition());
		PDL.add(PD);
		accept(TokenType.ID);
		while (_currentToken.getTokenType() == TokenType.COMMA){
			accept(TokenType.COMMA);
			TD = parseType();
			PD = new ParameterDecl(TD, _currentToken.getTokenText(), _currentToken.getTokenPosition());
			PDL.add(PD);
			accept(TokenType.ID);
		}
		return PDL;
	}

	private Statement parseStatement(){


		switch (_currentToken.getTokenType()){
			case LCURLY:
				accept(TokenType.LCURLY);
				StatementList SL = new StatementList();
				BlockStmt blckRet = new BlockStmt(SL, _currentToken.getTokenPosition());
				while (_currentToken.getTokenType() != TokenType.RCURLY) {
					SL.add(parseStatement());
				}
				accept(TokenType.RCURLY);
				return blckRet;

			case RETURN:
				accept(TokenType.RETURN);
				if(_currentToken.getTokenType() != TokenType.SEMICOLON){
					ReturnStmt retRet = new ReturnStmt(parseExpression(), this._currentToken.getTokenPosition());
					accept(TokenType.SEMICOLON);
					return retRet;
				}
				accept(TokenType.SEMICOLON);
				return new ReturnStmt(null, this._currentToken.getTokenPosition());


			case IF:
				accept(TokenType.IF);
				accept(TokenType.LPAREN);
				Expression ifClause = parseExpression();
				accept(TokenType.RPAREN);
				Statement ifStatement = parseStatement();
				if(_currentToken.getTokenType() == TokenType.ELSE){
					accept(TokenType.ELSE);
					Statement elseStatement = parseStatement();
					return new IfStmt(ifClause, ifStatement, elseStatement, this._currentToken.getTokenPosition());
				}
				else{
					return new IfStmt(ifClause, ifStatement, this._currentToken.getTokenPosition());
				}
			case WHILE:
				accept(TokenType.WHILE);
				accept(TokenType.LPAREN);
				Expression cond = parseExpression();
				accept(TokenType.RPAREN);
				Statement wStatement = parseStatement();
				return new WhileStmt(cond, wStatement, this._currentToken.getTokenPosition());

			case THIS:
				//REFERENCE generation
				Reference ref = parseReference();
				switch (_currentToken.getTokenType()){
					case EQUALS:
						accept(TokenType.EQUALS);
						Expression assignExp = parseExpression();
						accept(TokenType.SEMICOLON);
						return new AssignStmt(ref, assignExp, this._currentToken.getTokenPosition());
					case LBLOCK:
						accept(TokenType.LBLOCK);
						Expression inExp = parseExpression();
						accept(TokenType.RBLOCK);
						accept(TokenType.EQUALS);
						Expression outExp = parseExpression();
						accept(TokenType.SEMICOLON);
						return new IxAssignStmt(ref, inExp, outExp, _currentToken.getTokenPosition());
					case LPAREN:
						accept(TokenType.LPAREN);
						ExprList ExpAL = new ExprList();
						if(_currentToken.getTokenType() != TokenType.RPAREN) {
							ExpAL = parseArgumentList();
						}
						accept(TokenType.RPAREN);
						accept(TokenType.SEMICOLON);
						return new CallStmt(ref, ExpAL, this._currentToken.getTokenPosition());
				}
				break;
			case INTEGER:
			case BOOLEAN:
				TypeDenoter t = parseType();
				String varName = this._currentToken.getTokenText();
				VarDecl varDec = new VarDecl(t, varName, this._currentToken.getTokenPosition());
				accept(TokenType.ID);
				accept(TokenType.EQUALS);
				Expression e = parseExpression();
				VarDeclStmt vds = new VarDeclStmt(varDec, e, this._currentToken.getTokenPosition());
				accept(TokenType.SEMICOLON);
				return vds;

			default:
				// can be type or reference
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.PERIOD) {

					Reference reff = parseReference();
					switch (_currentToken.getTokenType()) {
						case EQUALS:
							accept(TokenType.EQUALS);
							Expression assignExp = parseExpression();
							accept(TokenType.SEMICOLON);
							return new AssignStmt(reff, assignExp, this._currentToken.getTokenPosition());

						case LBLOCK:
							accept(TokenType.LBLOCK);
							Expression inExp = parseExpression();
							accept(TokenType.RBLOCK);
							accept(TokenType.EQUALS);
							Expression outExp = parseExpression();
							accept(TokenType.SEMICOLON);
							return new IxAssignStmt(reff, inExp, outExp, _currentToken.getTokenPosition());

						case LPAREN:
							accept(TokenType.LPAREN);
							ExprList ExpAL = new ExprList();
							if(_currentToken.getTokenType() != TokenType.RPAREN) {
								ExpAL = parseArgumentList();
							}
							accept(TokenType.RPAREN);
							accept(TokenType.SEMICOLON);
							return new CallStmt(reff, ExpAL, this._currentToken.getTokenPosition());
					}
				} else if (_currentToken.getTokenType() == TokenType.LPAREN) {
					Reference reff = parseReference();
					accept(TokenType.LPAREN);
					ExprList AList = new ExprList();
					if(_currentToken.getTokenType() != TokenType.RPAREN) {
						AList = parseArgumentList();
					}
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOLON);
					return new CallStmt(reff, AList,_prevToken.getTokenPosition());

				} else if (_currentToken.getTokenType() == TokenType.EQUALS){
					Reference reff = parseReference();

					accept(TokenType.EQUALS);
					Expression exp = parseExpression();
					accept(TokenType.SEMICOLON);
					return new AssignStmt(reff, exp, _prevToken.getTokenPosition());

					}
				else if (_currentToken.getTokenType() == TokenType.ID) {
					//first id was type this is the id Type id = Expression;




					ClassType t2 = new ClassType(new Identifier(this._prevToken), this._prevToken.getTokenPosition());

					String varName2 = this._currentToken.getTokenText();
					VarDecl varDec2 = new VarDecl(t2, varName2, this._currentToken.getTokenPosition());

					accept(TokenType.ID);

					accept(TokenType.EQUALS);
					Expression e2 = parseExpression();
					VarDeclStmt vds2 = new VarDeclStmt(varDec2, e2, this._currentToken.getTokenPosition());
					accept(TokenType.SEMICOLON);
					return vds2;

				} else if (_currentToken.getTokenType() == TokenType.LBLOCK) {
					// justin case!
					IdRef justRef= new IdRef(new Identifier(_prevToken), _prevToken.getTokenPosition());
					Token hold = _prevToken;
					ClassType justClass = new ClassType(new Identifier(this._prevToken), this._prevToken.getTokenPosition());
					accept(TokenType.LBLOCK);
					if(_currentToken.getTokenType() == TokenType.RBLOCK){
						//TYPE
						ArrayType ari = new ArrayType(justClass, _currentToken.getTokenPosition());

						accept(TokenType.RBLOCK);

						VarDecl vard = new VarDecl(ari, _currentToken.getTokenText(), _currentToken.getTokenPosition());

						accept(TokenType.ID);
						accept(TokenType.EQUALS);
						Expression ep = parseExpression();
						accept(TokenType.SEMICOLON);
						return new VarDeclStmt(vard, ep, _prevToken.getTokenPosition());
					}
					else {
						Expression blockExp = parseExpression();
						accept(TokenType.RBLOCK);
						accept(TokenType.EQUALS);
						Expression signExp = parseExpression();
						accept(TokenType.SEMICOLON);
						return new IxAssignStmt(justRef, blockExp, signExp, _prevToken.getTokenPosition());
					}



		}
				else{
					_errors.reportError(String.format("Invalid statement at %d, %d", _scanner.line, _scanner.column));
					return null;
				}



		}

		return null;
	}

	private ExprList parseArgumentList(){
		ExprList EList = new ExprList();
		EList.add(parseExpression());
		while(_currentToken.getTokenType() == TokenType.COMMA){
			accept(TokenType.COMMA);
			EList.add(parseExpression());
		}

		return EList;
	}

	private Reference parseReference(){
		if (_currentToken.getTokenType() == TokenType.ID){
			accept(TokenType.ID);
			if (_currentToken.getTokenType() != TokenType.PERIOD){
				IdRef idRef = new IdRef(new Identifier(_prevToken), _prevToken.getTokenPosition());
				return idRef;
			}
		}
		else if (_currentToken.getTokenType() == TokenType.THIS) {
			accept(TokenType.THIS);
			if (_currentToken.getTokenType() != TokenType.PERIOD){
				ThisRef tRef = new ThisRef(_prevToken.getTokenPosition());
				return tRef;
			}
		}
		Reference old = null;
		if(_prevToken.getTokenType() == TokenType.ID){
			old  = new IdRef(new Identifier(_prevToken), _prevToken.getTokenPosition());
		}
		else{
			old = new ThisRef(_prevToken.getTokenPosition());
		}
		QualRef qually = null;
		while(_currentToken.getTokenType() == TokenType.PERIOD){
			accept(TokenType.PERIOD);
			qually = new QualRef(old, new Identifier(_currentToken), _currentToken.getTokenPosition());
			accept(TokenType.ID);
			old = qually;
		}
		if (qually == null){
			return old;
		}
		return qually;
	}
	private Expression parseExpression(){
		Expression retting = null;
		switch (_currentToken.getTokenType()){
			case INTLITERAL:
				accept(TokenType.INTLITERAL);
				retting = new LiteralExpr(new IntLiteral(_prevToken), _prevToken.getTokenPosition());
				break;
			case TRUE:
				accept(TokenType.TRUE);
				retting = new LiteralExpr(new BooleanLiteral(_prevToken), _prevToken.getTokenPosition());
				break;
			case FALSE:
				accept(TokenType.FALSE);
				retting = new LiteralExpr(new BooleanLiteral(_prevToken), _prevToken.getTokenPosition());
				break;
			case LPAREN:
				accept(TokenType.LPAREN);
				Expression inner = parseExpression();
				accept(TokenType.RPAREN);
				retting = inner;
				break;
			case OPERATOR:
				if(_currentToken.getTokenText().equals("!") || _currentToken.getTokenText().equals("-")) {
					Operator unop = new Operator(_currentToken);
					accept(TokenType.OPERATOR);

					Expression ex = parseExpression();
					if (ex instanceof  BinaryExpr ){
						retting = ex;
						if (((BinaryExpr) ex).left instanceof BinaryExpr){
							while (((BinaryExpr) ex).left instanceof BinaryExpr){
								ex = ((BinaryExpr) ex).left;
							}
							((BinaryExpr) ex).left = new UnaryExpr(unop, ((BinaryExpr) ex).left, _prevToken.getTokenPosition());
						}
						((BinaryExpr) ex).left = new UnaryExpr(unop, ((BinaryExpr) ex).left, _prevToken.getTokenPosition());
					}
					else {
						retting = new UnaryExpr(unop, ex, _prevToken.getTokenPosition());
					}
				}
				else{
					_errors.reportError("Expected unary operator");
				}
				break;
			case NEW:

				accept(TokenType.NEW);
				switch (_currentToken.getTokenType()){
					case ID:
						accept(TokenType.ID);
						if (_currentToken.getTokenType() == TokenType.LPAREN){
							ClassType inter = new ClassType(new Identifier(_prevToken), _prevToken.getTokenPosition());
							accept(TokenType.LPAREN);
							accept(TokenType.RPAREN);
							retting = new NewObjectExpr(inter, _prevToken.getTokenPosition());
						}
						else/* (_currentToken.getTokenType() == TokenType.LBLOCK) */{
							ClassType inter = new ClassType(new Identifier(_prevToken), _prevToken.getTokenPosition());
							accept(TokenType.LBLOCK);
							Expression e = parseExpression();
							accept(TokenType.RBLOCK);
							retting = new NewArrayExpr(inter, e, _prevToken.getTokenPosition());
						}
						break;
					default:
						BaseType inter = new BaseType(TypeKind.INT, _currentToken.getTokenPosition());
						accept(TokenType.INTEGER);
						accept(TokenType.LBLOCK);
						Expression e = parseExpression();
						accept(TokenType.RBLOCK);
						retting = new NewArrayExpr(inter, e, _prevToken.getTokenPosition());
				}
				break;
			case ID:
			case THIS:
				Reference reff = parseReference();

				if (_currentToken.getTokenType() == TokenType.LPAREN){
					accept(TokenType.LPAREN);
					ExprList aList = new ExprList();
					if (_currentToken.getTokenType() != TokenType.RPAREN){
						aList = parseArgumentList();
					}
					accept(TokenType.RPAREN);
					retting = new CallExpr(reff, aList, _prevToken.getTokenPosition());

				} else if (_currentToken.getTokenType() == TokenType.LBLOCK) {
					accept(TokenType.LBLOCK);
					Expression exP = parseExpression();
					accept(TokenType.RBLOCK);
					retting = new IxExpr(reff, exP, _prevToken.getTokenPosition());

				}
				else {
					retting = new RefExpr(reff, _prevToken.getTokenPosition());
				}

				break;
			default:
				_errors.reportError("Invalid expression at " + _scanner.line + ", " + _scanner.column);


		}


		int initPrec = precedence_level;
		int currentPrec = precedence_level;
		while(_currentToken.getTokenType() == TokenType.OPERATOR && !_currentToken.getTokenText().equals("!") ){

			if( precedence_level < precedenceTable()){
				Operator ops = new Operator(_currentToken);
				precedence_level = precedenceTable();
				accept(TokenType.OPERATOR);
				BinaryExpr binexp = new BinaryExpr(ops, retting, parseExpression(), _prevToken.getTokenPosition());
				retting = binexp;
				precedence_level = currentPrec;
			}
			else{
				precedence_level = initPrec; // change precedence level to a linked list stack so it doesnt full reset here
				return retting;
			}


		}
		return retting;

	}

	private int precedenceTable(){
		switch (_currentToken.getTokenText()){
			case "||":
				return 1;
			case "&&":
				return 2;
			case "==":
			case "!=":
				return 3;
			case "<=":
			case "<":
			case ">":
			case ">=":
				return 4;
			case "-":
			case"+":
				return 5;
			case "*":
			case "/":
				return 6;

		}
		return -1;
	}

	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType ) {
			_prevToken = _currentToken;
			_currentToken = _scanner.scan();
			return;
		}

		
		// TODO: Report an error here.
		//  "Expected token X, but got Y"
		//throw new SyntaxError();
		if (_currentToken.getTokenType() == TokenType.UNCLOSEDCOMMENTBLOCK){
			_errors.reportError(String.format("Unclosed block comment at %d, %d", _scanner.line, _scanner.column));
		}
		else {
			_errors.reportError(String.format("Expected %s, but got %s at %d, %d", expectedType.name(), _currentToken.getTokenText(), _scanner.line, _scanner.column));

		}
		throw new SyntaxError();
	}
}

