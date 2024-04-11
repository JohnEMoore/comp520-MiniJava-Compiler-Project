package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;

import java.util.HashMap;
import java.util.Stack;

public class ScopedIdentification {

    public AST curtree;

    private Stack<HashMap<String, Declaration>> siStack;

    public boolean classRef = false;

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
                throw new IdentificationError(curtree, "Variable name already exists in current scope");
            }
        }
        siStack.peek().put(name, decl);
    }

    public Declaration findDeclaration(Identifier id, Declaration context){
            Declaration ret = null;
            if (classRef){
                if (siStack.elementAt(0).get(id.spelling) != null){
                    ret = siStack.elementAt(0).get(id.spelling);
                    return ret;
                }
            }

            for (int i = siStack.size() - 1; i >= 0; i --){
                if (i == 1){

                    if(context.getClass() == FieldDecl.class){
                        //System.out.println(((ClassType)((FieldDecl) context).type).className.spelling + "." + id.spelling);
                        if (siStack.elementAt(i).get( ((ClassType)((FieldDecl) context).type).className.spelling + "." + id.spelling) != null) {
                            ret = siStack.elementAt(i).get( ((ClassType)((FieldDecl) context).type).className.spelling + "." + id.spelling);
                            //return ret;
                        }
                    }
                    else if(context.getClass() == VarDecl.class){
                        if (siStack.elementAt(i).get( ((ClassType)((VarDecl) context).type).className.spelling + "." + id.spelling) != null) {
                            ret = siStack.elementAt(i).get( ((ClassType)((VarDecl) context).type).className.spelling + "." + id.spelling);
                           // return ret;
                        }
                    }
                    else{
                        if (siStack.elementAt(i).get(context.name + "." + id.spelling) != null) {
                            ret = siStack.elementAt(i).get(context.name + "." + id.spelling);
                          //  return ret;
                        }
                    }
                }
                if (siStack.elementAt(i).get(id.spelling) != null){
                    ret = siStack.elementAt(i).get(id.spelling);
                   // return ret;
                }
            }
            return ret;
    }
}
