/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGeneration.x64.Reg64;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Reference extends AST
{
	public int entityOffset;
	public Reg64 entityRef;
	public Reference(SourcePosition posn){
		super(posn);
	}

}
