package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;

import java.util.HashMap;
import java.util.Stack;

public class ScopedIdentification {


    private Stack<HashMap<String, Declaration>> siStack;

    public ScopedIdentification(){
        siStack = new Stack<HashMap<String, Declaration>>();
        HashMap<String, Declaration> classMap = new HashMap<>();
        siStack.push(classMap);
    }

    public void openScope(){
        HashMap<String, Declaration> newScope = new HashMap<>();
        siStack.push(newScope);
    }

    public void closeScope(){
        siStack.pop();
    }

    public void addDeclaration(String name, Declaration decl){
        for (int i = siStack.size() - 1; i >= 2 ? (i >= 2) : i >= siStack.size() - 1; i --){
            if (siStack.elementAt(i).get(name) != null){
                throw new Error("IdentificationError");
            }
        }
        siStack.peek().put(name, decl);
    }

    public Declaration findDeclaration(Identifier id, String context){
            Declaration ret = null;
            for (int i = siStack.size() - 1; i >= 0; i --){
                if (i == 1){
                    if (siStack.elementAt(i).get(id.spelling + context) != null){
                        ret = siStack.elementAt(i).get(context + "." + id.spelling);
                    }
                }
                if (siStack.elementAt(i).get(id.spelling) != null){
                    ret = siStack.elementAt(i).get(id.spelling);
                }
            }
            return ret;
    }
}
