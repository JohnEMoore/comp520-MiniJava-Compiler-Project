package miniJava.CodeGeneration;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

public class CodeGenerator implements Visitor<Object, Object> {
	private final ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section


	private int mainLoc;
	private int staticStack = 0;
	private int RBPoffset = -8;
	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse(Package prog) {
		_asm = new InstructionList();
		
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		
		prog.visit(this,null);
		
		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
	}


	/*
	getting started:
	do stack framing
	variable declarations
	offset from rbp

	var decl: store with a variable - 8, then - 16

	visit method parameters in reverse order,
	decorate each parameter, so they go from +16, to 24, 32, etc
	this should be at rbp + 16
	because rbp has old rbp
	rbp + 8 is return address

	for variables that aren't static


	can use lea to load (in lecture notes)
	 */

	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO: visit relevant parts of our AST


		ClassDeclList cl = prog.classDeclList;
		_asm.add( new Mov_rrm(		new R(Reg64.R15,Reg64.RSP)) 	); // set reg 15 to current location
		_asm.add(new Sub( new R(Reg64.R15, true), 8)); // set R15 to be at the location of the next thing I push (static var)
		for (ClassDecl c: prog.classDeclList){
			c.visit(this, null);
		}
		return null;

	}

	public Object visitClassDecl(ClassDecl clas, Object arg){
		int i = 0;

		for (FieldDecl f: clas.fieldDeclList) {
			//TODO take type from nonstatic fields, use it to calculate offset for that and subsequent fields (alternatively this could be done during the run each call)
			//TODO take static fields and place in stack

			if (!f.isStatic) {
				f.entityOffset = i;
				i += f.type.getClass() == BaseType.class ? 4 : 8;
				f.visit(this, null);
			}
			else{
				f.visit(this, null);
				f.entityOffset = staticStack;
				_asm.add( new Push(0) ); // fields are not declared in minijava
				staticStack -= 8;


			}

		}

		// visit all methodDecls, generate their code, if there is a call to a
		for (MethodDecl m: clas.methodDeclList) {
			if(m.name.equals("main") &&  m.isStatic && !m.isPrivate && m.parameterDeclList.size() == 1 &&  m.parameterDeclList.get(0).type.getClass() == ArrayType.class && ((ArrayType)  m.parameterDeclList.get(0).type).eltType.getClass() == ClassType.class && ((ClassType) ((ArrayType)  m.parameterDeclList.get(0).type).eltType ).className.spelling.equals("String")){
				mainLoc = _asm.getSize();
			}
			m.instructionLocation = _asm.getSize();
			m.visit(this, null);
		}


		return null;
	}

	public Object visitFieldDecl(FieldDecl f, Object arg){
		f.type.visit(this, null);
		return null;
	}

	public Object visitMethodDecl(MethodDecl m, Object arg){
		RBPoffset = -8; // resets RBP offset at start of method, need to get trimmed to pass all classes
		m.type.visit(this, null); // dont think this matters
		ParameterDeclList pdl = m.parameterDeclList;
		int i = 16; // first param is at RBP + 16
		if(!m.isStatic){
			// get this ref at RBP + 16
			i += 8;
		}

		for (ParameterDecl pd: pdl) {
			//dont need to place on stack, just know where each would be on stack relative to rbp when this gets called
			//pd.entityRef = Reg64.RBP; using default of field since locals should all use rbp - will keep if no errors occur
			pd.entityOffset = i;
			i += 8;

			//pd.visit(this, null);
		}
		StatementList sl = m.statementList; // have to generate code per statement
		for (Statement s: sl) {
			s.visit(this, null);
		}
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, Object arg){
		pd.type.visit(this, null);

		return null;
	}

	public Object visitVarDecl(VarDecl vd, Object arg){
		vd.type.visit(this, null);
		_asm.add( new Push(0) ); // push var on to stack
		vd.entityOffset = RBPoffset; // set offset location and decrement offset
		RBPoffset -= 8;
		return null;
	}


	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitBaseType(BaseType type, Object arg){
		return null;
	}

	public Object visitClassType(ClassType ct, Object arg){
		ct.className.visit(this, null);
		return null;
	}

	public Object visitArrayType(ArrayType type, Object arg){
		type.eltType.visit(this, null);
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitBlockStmt(BlockStmt stmt, Object arg){
		StatementList sl = stmt.sl;
		int scopeStart = RBPoffset;
		for (Statement s: sl) {
			s.visit(this,null);
		}

		_asm.add( new Sub(		new R(Reg64.RSP, true), RBPoffset - scopeStart )); // reclaim stack space
		RBPoffset = scopeStart; // set RBP offset to where it was prior to scope.
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg){
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		//_asm.add( new Pop(Reg64.RAX) ); // have to get value from the expression  EXP IN RAX
		_asm.add(new Mov_rmr(new R(Reg64.RBP, stmt.varDecl.entityOffset, Reg64.RAX))); // move value RAX to [RBP - offset]
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object arg){
		stmt.ref.visit(this, null);
		Class stmtClass = stmt.ref.getClass();
		stmt.val.visit(this, null);
		//_asm.add( new Pop(Reg64.RAX) ); // have to get value from the expression

		_asm.add(new Mov_rmr(new R(stmt.ref.entityRef, stmt.ref.entityOffset, Reg64.RAX))); // move value RAX to [<ref location> - offset]
		return null;
	}

	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg){
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, Object arg){
		stmt.methodRef.visit(this, null);
		ExprList al = stmt.argList;


		for (int i = al.size() -1; i >= 0; i--) {
			al.get(i).visit(this, null);
			_asm.add( new Push(Reg64.RAX) ); //load params onto stack in reverse order
		}
		//TODO check if method isnt static and if so, push object pointer onto stack
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object arg){
		if (stmt.returnExpr != null)
			stmt.returnExpr.visit(this, null);
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object arg){
		stmt.cond.visit(this, null);
		stmt.thenStmt.visit(this, null);
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, null);
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object arg){
		stmt.cond.visit(this, null);
		stmt.body.visit(this, null);
		return null;
	}

///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitUnaryExpr(UnaryExpr expr, Object arg){
		expr.operator.visit(this, null);
		expr.expr.visit(this, null);
		//_asm.add( new Pop(Reg64.RAX) ); // have to get value from the expression for expr.expr
		if(expr.operator.spelling.equals("!")){
			_asm.add(new Not(new R(Reg64.RAX, true)));
		}
		else{ //-
			_asm.add(new Neg(new R(Reg64.RAX, true)));
		}
		//_asm.add(new Push(new R(Reg64.RSP, Reg64.RAX))); // store result on stack STORED IN RAX

		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, Object arg){
		expr.operator.visit(this, null);
		expr.left.visit(this, null);
		_asm.add(new Push(new R(Reg64.RSP, Reg64.RAX))); // store result on stack
		expr.right.visit(this, null);
		_asm.add(new Push(new R(Reg64.RSP, Reg64.RAX))); // store result on stack

		_asm.add( new Pop(Reg64.RCX) ); // put top of stack (rhs result) in rcx
		_asm.add( new Pop(Reg64.RAX) ); // put top of stack (lhs result) in rax

		switch (expr.operator.spelling){
			//TODO add expression evaluations
			case "&&":
			case "||":

			case ">":
			case "<":
			case "<=":
			case ">=":

			case "+":
			case"-":
			case"*":
			case"/":

			case "==":
			case "!=":

		}

		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object arg){
		expr.ref.visit(this, null);
		return null;
	}

	public Object visitIxExpr(IxExpr ie, Object arg){
		ie.ref.visit(this, null);
		ie.ixExpr.visit(this, null);
		return null;
	}

	public Object visitCallExpr(CallExpr expr, Object arg){
		expr.functionRef.visit(this, null);
		ExprList al = expr.argList;
		for (int i = al.size() -1; i >= 0; i--) {
			al.get(i).visit(this, null);
			_asm.add( new Pop(Reg64.RAX) ); //load params onto stack in reverse order
		}
		//TODO check if method isnt static and if so, push object pointer onto stack

		//TODO call the method
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, Object arg){
		expr.lit.visit(this, null);
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg){
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg){
		expr.classtype.visit(this, null);
		return null;
	}


	///////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitThisRef(ThisRef ref, Object arg) {
		return null;
	}

	public Object visitIdRef(IdRef ref, Object arg) {
		ref.id.visit(this, null);
		return null;
	}

	public Object visitQRef(QualRef qr, Object arg) {
		qr.id.visit(this, null);
		qr.ref.visit(this, null);
		return null;
	}


	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitIdentifier(Identifier id, Object arg){
		return null;
	}

	public Object visitOperator(Operator op, Object arg){
		return null;
	}

	public Object visitIntLiteral(IntLiteral num, Object arg){
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg){
		return null;
	}

	public Object visitNullLiteral (NullLiteral n1, Object arg){
		return null;
	}










	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), mainLoc);// TODO: set the location of the main method
	}
	
	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new R(Reg64.RAX,true),0x09) ); // mmap
		
		_asm.add( new Xor(		new R(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		_asm.add( new Mov_rmi(	new R(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(	new R(Reg64.RDX,true),0x03) 	); // prot read|write
		_asm.add( new Mov_rmi(	new R(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		_asm.add( new Mov_rmi(	new R(Reg64.R8, true),-1) 	); // fd= -1
		_asm.add( new Xor(		new R(Reg64.R9,Reg64.R9)) 	); // offset=0
		_asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}
	
	private int makePrintln() {
		// TODO: how can we generate the assembly to println?

		int idxStart = _asm.add( new Mov_rmi(new R(Reg64.RAX,true),0x01) ); // call 1 WRITE
		//_asm.add( new Mov_rrm(new R(Reg64.RDI, true))); // TODO function arg + 48 into RDI
		_asm.add( new Mov_rmi(	new R(Reg64.RSI,true),0x1000) ); // character
		_asm.add( new Mov_rmi(	new R(Reg64.RDX,true),0x03) 	);


		return -1;
	}
}



/*
DOING CALLS:
every method has SF start and end.



function reference:
if static: not THIS, either if or qual ref

must be qual ref:
lhs ref: will act as/become "this" for function call for the rhs
rhs id

inside method, don't know where you are
this should be rbp + 16

special check, if static dont push this

so doing a call ref / call statement
IF ID REF:
	if in static:
		do as normal function call
	else:
		push variable you have from stack (rbp +16), variable from stack



decorate method decls, with instruction locations set at -1
note decls that aren't known and patch later when found
 */
