/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGeneration.x64.Reg64;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class LocalDecl extends Declaration {

	public Reg64 entityRef = Reg64.RBP;
	public int entityOffset;
	
	public LocalDecl(String name, TypeDenoter t, SourcePosition posn){
		super(name,t,posn);
	}

}
