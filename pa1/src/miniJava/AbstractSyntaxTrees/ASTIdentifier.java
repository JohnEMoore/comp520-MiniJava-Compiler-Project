/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.ScopedIdentification;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import javax.xml.stream.FactoryConfigurationError;

/*
 * Display AST in text form, one node per line, using indentation to show
 * subordinate nodes below a parent node.
 *
 * Performs an in-order traversal of AST, visiting an AST node of type XXX
 * with a method of the form
 *
 *       public Object visitXXX( XXX astnode, String arg)
 *
 *   where arg is a prefix string (indentation) to precede display of ast node
 *   and a null Object is returned as the result.
 *   The display is produced by printing a line of output at each node visited.
 */
public class ASTIdentifier implements Visitor<String,Object> {

    public ScopedIdentification SId = new ScopedIdentification();
    Declaration curClass;
    public static boolean showPosition = false;
    public AST currentTree = null;
    public String currentVarDecl;
    public boolean inPrivate = false;
    public boolean inStatic = false;
    public boolean ownScope = false;
    public boolean privIssue = false;

    public boolean hasMain = false;

    public Object retType = TypeKind.VOID;

    /**
     * print text representation of AST to stdout
     *
     * @param ast root node of AST
     */
    public void showTree(AST ast) {
        currentTree = ast;
        //System.out.println("======= AST Display =========================");
        ast.visit(this, "");
        //System.out.println("=============================================");
    }

    private void PreloadInit() {
        SourcePosition defPosn = new SourcePosition(0, 0);
        FieldDeclList PsFields = new FieldDeclList();
        MethodDeclList PsMethods = new MethodDeclList();
        ParameterDecl PsParam = new ParameterDecl(new BaseType(TypeKind.INT, defPosn), "n", defPosn);
        ParameterDeclList PsParamList = new ParameterDeclList();
        PsParamList.add(PsParam);
        MethodDecl PsMethod = new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, new SourcePosition(0, 0)), "println",  new SourcePosition(0, 0)), PsParamList, new StatementList(), defPosn);
        PsMethods.add(PsMethod);
        SId.addDeclaration("_PrintStream", new ClassDecl("_PrintStream", PsFields, PsMethods, defPosn));

        FieldDeclList SysFields = new FieldDeclList();
        Token toke = new Token(TokenType.ID, "_PrintStream", defPosn);
        FieldDecl fs = new FieldDecl(false, true, new ClassType(new Identifier(toke), defPosn),"out", defPosn);
        SysFields.add(fs);
        ClassDecl SystemDecl = new ClassDecl("System", SysFields, new MethodDeclList(), defPosn);
        SId.addDeclaration("System", SystemDecl);
        SId.addDeclaration("String", new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), defPosn));
        SId.openScope();

        SId.addDeclaration("System.out",fs );
        SId.addDeclaration("_PrintStream.println", PsMethod);
    }



    // methods to format output

    /**
     * display arbitrary text for a node
     * @param prefix  indent text to indicate depth in AST
     * @param text    preformatted node display
     */
    private void show(String prefix, String text) {
        //System.out.println(prefix + text);
    }

    /**
     * display AST node by name
     * @param prefix  spaced indent to indicate depth in AST
     * @param node    AST node, will be shown by name
     */
    private void show(String prefix, AST node) {
        // System.out.println(prefix + node.toString());
    }

    /**
     * quote a string
     * @param text    string to quote
     */
    private String quote(String text) {
        return ("\"" + text + "\"");
    }

    /**
     * increase depth in AST
     * @param prefix  current spacing to indicate depth in AST
     * @return  new spacing
     */
    private String indent(String prefix) {
        return prefix + "  ";
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Object visitPackage(Package prog, String arg) throws Error {
        SId.curtree = currentTree;
        show(arg, prog);
        ClassDeclList cl = prog.classDeclList;
        show(arg,"  ClassDeclList [" + cl.size() + "]");
        String pfx = arg + "  . ";
        for (ClassDecl c: prog.classDeclList){
            visitFirstClassDecl(c);

        }
        PreloadInit();
        for (ClassDecl c: prog.classDeclList){
            visitmembersFirst(c, arg);

        }
        for (ClassDecl c: prog.classDeclList) {
            curClass = c;
            for (MethodDecl m : c.methodDeclList) {
                visitMethodDeclFirst(m);

            }
        }

        for (ClassDecl c: prog.classDeclList){
            c.visit(this, pfx);
        }

        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Object visitFirstClassDecl(ClassDecl clas){
        SId.addDeclaration(clas.name, clas);
        return null;
    }
    public Object visitClassDecl(ClassDecl clas, String arg){
        show(arg, clas);
        show(indent(arg), quote(clas.name) + " classname");
        curClass = clas;
        show(arg,"  FieldDeclList [" + clas.fieldDeclList.size() + "]");
        String pfx = arg + "  . ";
        show(arg,"  MethodDeclList [" + clas.methodDeclList.size() + "]");

        for (MethodDecl m: clas.methodDeclList) {
            m.visit(this, pfx);
        }

        if(!hasMain){
            throw new IdentificationError(currentTree, "No public static void main(String[]) found");
        }

        return null;
    }

    public Object visitmembersFirst(ClassDecl clas, String pfx){
        curClass = clas;
        for (FieldDecl f: clas.fieldDeclList)
            visitFieldDecl(f, pfx);
        return  null;
    }

    public Object visitFieldDecl(FieldDecl f, String arg){
        show(arg, "(" + (f.isPrivate ? "private": "public")
                + (f.isStatic ? " static) " :") ") + f.toString());
        f.type.visit(this, indent(arg));
        show(indent(arg), quote(f.name) + " fieldname");
        SId.addDeclaration(curClass.name + "." + f.name, f);
        return null;
    }

    public Object visitMethodDecl(MethodDecl m, String arg){
        SId.openScope(); // move to scope 2
        inPrivate = m.isPrivate;
        inStatic = m.isStatic;
        show(arg, "(" + (m.isPrivate ? "private": "public")
                + (m.isStatic ? " static) " :") ") + m.toString());
        retType = m.type.visit(this, indent(arg));
        show(indent(arg), quote(m.name) + " methodname");
        ParameterDeclList pdl = m.parameterDeclList;
        show(arg, "  ParameterDeclList [" + pdl.size() + "]");
        String pfx = ((String) arg) + "  . ";

        for (ParameterDecl pd: pdl) {
            pd.visit(this, pfx);
        }
        StatementList sl = m.statementList;
        if(sl.get(sl.size() - 1).getClass() != ReturnStmt.class && m.type.typeKind != TypeKind.VOID){
            throw new IdentificationError(currentTree, "no return");
        }
        show(arg, "  StmtList [" + sl.size() + "]");
        for (Statement s: sl) {
            s.visit(this, pfx);
        }
        if( m.name.equals("main") &&  m.isStatic && !m.isPrivate && pdl.size() == 1 && pdl.get(0).type.getClass() == ArrayType.class && ((ArrayType) pdl.get(0).type).eltType.getClass() == ClassType.class && ((ClassType) ((ArrayType) pdl.get(0).type).eltType ).className.spelling.equals("String") ){
            if(hasMain){
                throw new IdentificationError(currentTree, "cant have multiple main methods");
            }
            hasMain = true;
        }

        inPrivate = false;
        inStatic = false;
        SId.closeScope();
        return null;
    }

    public Object visitMethodDeclFirst(MethodDecl m){
        SId.addDeclaration(curClass.name + "." + m.name, m); // me

        return null;
    }


    public Object visitParameterDecl(ParameterDecl pd, String arg){
        show(arg, pd);
        SId.addDeclaration(pd.name, pd);
        pd.type.visit(this, indent(arg));
        show(indent(arg), quote(pd.name) + "parametername ");
        return null;
    }

    public Object visitVarDecl(VarDecl vd, String arg){
        if (ownScope){
            throw new Error("Cant have own scope");
        }
        SId.addDeclaration(vd.name, vd);
        show(arg, vd);
        vd.type.visit(this, indent(arg));
        show(indent(arg), quote(vd.name) + " varname");
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    ///////////////////////////////////////////////////////////////////////////////

    public TypeKind visitBaseType(BaseType type, String arg){
        show(arg, type.typeKind + " " + type.toString());
        return type.typeKind;
    }

    public TypeKind visitClassType(ClassType ct, String arg){
        show(arg, ct);
        SId.classRef = true;
        Declaration ret;
        ret = visitIdentifier(ct.className, indent(arg));
        if (ret.getClass() != ClassDecl.class){
            throw new IdentificationError(this.currentTree, "Class not declared " + ret.name);
        }
        SId.classRef = false;
        return ct.typeKind;
    }

    public TypeKind visitArrayType(ArrayType type, String arg){
        show(arg, type);
        type.eltType.visit(this, indent(arg));
        return type.typeKind;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Object visitBlockStmt(BlockStmt stmt, String arg){
        SId.openScope();
        boolean temp = ownScope;
        ownScope = false;
        show(arg, stmt);
        StatementList sl = stmt.sl;
        show(arg,"  StatementList [" + sl.size() + "]");
        String pfx = arg + "  . ";
        for (Statement s: sl) {
            s.visit(this, pfx);
        }
        SId.closeScope();
        ownScope = temp;
        return null;
    }

    public Object visitVardeclStmt(VarDeclStmt stmt, String arg){

        show(arg, stmt);

        stmt.varDecl.visit(this, indent(arg));
        currentVarDecl = stmt.varDecl.name;

        TypeDenoter ExpType = (TypeDenoter) stmt.initExp.visit(this, indent(arg));


        // put type of var into the indentifier
        currentVarDecl = "";

        if (stmt.varDecl.type.typeKind != ExpType.typeKind && ExpType.typeKind != TypeKind.NULL){
            if(stmt.varDecl.type.typeKind == TypeKind.ARRAY && ((ArrayType) stmt.varDecl.type).eltType.typeKind == ExpType.typeKind ){
                return null;
            }
            throw new IdentificationError(currentTree, "Assigned variable wrong type");
        }
        if (stmt.varDecl.type.typeKind == TypeKind.CLASS && ExpType.getClass() == ClassType.class && ! ((ClassType) stmt.varDecl.type).className.spelling.equals( ((ClassType) ExpType).className.spelling)){
            throw new IdentificationError(currentTree, "Assigned variable wrong type");
        }

        return null;
    }

    public Object visitAssignStmt(AssignStmt stmt, String arg){
        show(arg,stmt);
        Declaration refDecl = (Declaration)  stmt.ref.visit(this, indent(arg));
        TypeDenoter valType = (TypeDenoter) stmt.val.visit(this, indent(arg));
        if (valType == null){
            throw new IdentificationError(currentTree, "Invalid type assignment");
        }
        else if(valType.typeKind != refDecl.type.typeKind){
            if (!(refDecl.type.getClass().equals(ArrayType.class) && stmt.val.getClass().equals(NewArrayExpr.class) && ((ArrayType) refDecl.type).eltType.typeKind.equals(valType.typeKind) )){
                if( valType.typeKind != TypeKind.NULL ) {
                    throw new IdentificationError(currentTree, "Invalid type assignment");
                }
            }


        }

        return null;
    }

    public Object visitIxAssignStmt(IxAssignStmt stmt, String arg){
        show(arg,stmt);
        stmt.ref.visit(this, indent(arg));
        stmt.ix.visit(this, indent(arg));
        stmt.exp.visit(this, indent(arg));
        return null;
    }

    public Object visitCallStmt(CallStmt stmt, String arg){
        show(arg,stmt);
        Object refd = stmt.methodRef.visit(this, indent(arg));
        if (refd.getClass() != MethodDecl.class ){
            //meant to fix fail337
            throw new IdentificationError(currentTree, "Use this for method call");
        }

        ExprList al = stmt.argList;
        show(arg,"  ExprList [" + al.size() + "]");
        String pfx = arg + "  . ";
        int i = 0;
        for (Expression e: al) {
            TypeDenoter type = (TypeDenoter) e.visit(this, pfx);
            if ( type.typeKind != ((MethodDecl) refd).parameterDeclList.get(i).type.typeKind){
                throw new IdentificationError(currentTree, "Improper type in arg list");
            }
            i += 1;
        }
        return null;
    }

    public Object visitReturnStmt(ReturnStmt stmt, String arg){
        show(arg,stmt);

        if (stmt.returnExpr != null) {
            TypeDenoter TD = (TypeDenoter) stmt.returnExpr.visit(this, indent(arg));
            if (retType != TD.typeKind){
                throw new IdentificationError(currentTree, "Return type doesn't match");
            }
        }
        else if (retType != TypeKind.VOID){
            throw new IdentificationError(currentTree, "Return type doesn't match");
        }
        return null;
    }

    public Object visitIfStmt(IfStmt stmt, String arg){
        show(arg,stmt);
        TypeDenoter condStmt = (TypeDenoter) stmt.cond.visit(this, indent(arg));
        if (condStmt.typeKind != TypeKind.BOOLEAN){
            throw new IdentificationError(currentTree, "If statement doesn't have type bool");
        }
        ownScope = true;
        stmt.thenStmt.visit(this, indent(arg));
        ownScope = false;
        if (stmt.elseStmt != null) {
            ownScope = true;
            stmt.elseStmt.visit(this, indent(arg));
            ownScope = false;
        }
        return null;
    }

    public Object visitWhileStmt(WhileStmt stmt, String arg){
        show(arg, stmt);
        stmt.cond.visit(this, indent(arg));
        ownScope = true;
        stmt.body.visit(this, indent(arg));
        ownScope = false;
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public TypeDenoter visitUnaryExpr(UnaryExpr expr, String arg){
        show(arg, expr);
        TypeDenoter ret;
        expr.operator.visit(this, indent(arg));
        ret = (TypeDenoter) expr.expr.visit(this, indent(indent(arg)));
        if (ret.typeKind == TypeKind.INT && expr.operator.spelling.equals("-")){
            return ret;
        }
        if(ret.typeKind == TypeKind.BOOLEAN && expr.operator.spelling.equals("!")){
            return ret;
        }
        throw new Error("Improper type for unary expression");
    }

    public TypeDenoter visitBinaryExpr(BinaryExpr expr, String arg){
        show(arg, expr);
        expr.operator.visit(this, indent(arg));
        TypeDenoter left = (TypeDenoter) expr.left.visit(this, indent(indent(arg)));
        TypeDenoter right = (TypeDenoter)expr.right.visit(this, indent(indent(arg)));
        switch (expr.operator.spelling){
            case "&&":
            case "||":
                if(left.typeKind == TypeKind.BOOLEAN && right.typeKind == TypeKind.BOOLEAN){
                    return new BaseType(TypeKind.BOOLEAN, expr.operator.posn);
                }
                else {
                    throw new Error("Improper boolean expression");
                }
            case ">":
            case "<":
            case "<=":
            case ">=":
                if(left.typeKind == TypeKind.INT && right.typeKind == TypeKind.INT){
                    return new BaseType(TypeKind.BOOLEAN, expr.operator.posn);
                }
                else {
                    throw new Error("Improper inequality expression");
                }
            case "+":
            case"-":
            case"*":
            case"/":
                if(left.typeKind == TypeKind.INT && right.typeKind == TypeKind.INT){
                    return new BaseType(TypeKind.INT, expr.operator.posn);
                }
                else {
                    throw new Error("Improper mathematical expression");
                }
            case "==":
            case "!=":
                if( left.typeKind == TypeKind.NULL || right.typeKind == TypeKind.NULL || left.typeKind == right.typeKind && (left.typeKind != TypeKind.CLASS || (((ClassType) left).className.spelling == ((ClassType) right).className.spelling) ) ){
                    return new BaseType(TypeKind.BOOLEAN, expr.operator.posn);
                }
                else {
                    throw new Error("Improper inequality expression");
                }
        }
        return null;
    }

    public TypeDenoter visitRefExpr(RefExpr expr, String arg){
        show(arg, expr);
        Declaration refdecl = (Declaration) expr.ref.visit(this, indent(arg));
        if (refdecl.getClass() == MethodDecl.class && !expr.getClass().equals( CallExpr.class)){
            throw new IdentificationError(currentTree, "no variable or reference with that name");
            // methods can be references
        }
        if (expr.ref.getClass() == ThisRef.class){
            return new BaseType(TypeKind.CLASS, expr.ref.posn);
        }
        return refdecl.type;
    }

    public TypeDenoter visitIxExpr(IxExpr ie, String arg){
        show(arg, ie);
        Declaration reffed = (Declaration) ie.ref.visit(this, indent(arg));
        TypeDenoter indexType = ((TypeDenoter)  ie.ixExpr.visit(this, indent(arg)));
        if (indexType.typeKind != TypeKind.INT){
            throw new IdentificationError(currentTree, "Index must be an int");
        }
        ie.ixExpr.visit(this, indent(arg));
        return ((ArrayType) reffed.type).eltType;
    }

    public TypeDenoter visitCallExpr(CallExpr expr, String arg){
        show(arg, expr);
        Declaration reffed = (Declaration) expr.functionRef.visit(this, indent(arg));
        if (reffed.getClass() != MethodDecl.class){
            throw new IdentificationError(currentTree, "call must be a method");
        }
        ExprList al = expr.argList;
        show(arg,"  ExprList + [" + al.size() + "]");
        String pfx = arg + "  . ";
        int i = 0;
        for (Expression e: al) {
            TypeDenoter type = (TypeDenoter) e.visit(this, pfx);
            if ( type.typeKind != ((MethodDecl) reffed).parameterDeclList.get(i).type.typeKind){
                throw new IdentificationError(currentTree, "Improper type in arg list");
            }
            i += 1;
        }
        return reffed.type;
    }

    public TypeDenoter visitLiteralExpr(LiteralExpr expr, String arg){
        show(arg, expr);
        return (TypeDenoter) expr.lit.visit(this, indent(arg));
    }

    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, String arg){
        show(arg, expr);
        expr.eltType.visit(this, indent(arg));
        expr.sizeExpr.visit(this, indent(arg));
        return expr.eltType;
    }

    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, String arg){
        show(arg, expr);
        expr.classtype.visit(this, indent(arg));
        return expr.classtype;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Declaration visitThisRef(ThisRef ref, String arg) {
        show(arg,ref);
        if(this.inStatic){
            throw new IdentificationError(currentTree, "Using this in a static method");
        }

        privIssue = false;
        return curClass;
    }

    public Declaration visitIdRef(IdRef ref, String arg) {
        show(arg,ref);
        Declaration decl = visitIdentifier(ref.id, indent(arg));
        if (decl.name.equals(currentVarDecl)){
            throw new IdentificationError(currentTree, "Cant use var in own decl statement");
        }
        if(this.inStatic && (decl.getClass() == FieldDecl.class && !((FieldDecl) decl).isStatic)){
            throw new IdentificationError(currentTree, "Nonstatic var in static method");
        }

        privIssue = false;



        if (decl == null) {
            throw new IdentificationError(currentTree, "Undeclared variable");
        }
        return decl;
    }


    public Declaration visitQRef(QualRef qr, String arg) {
        Declaration ref = null;
        Declaration rhs;
        show(arg, qr);
        if (qr.ref.getClass() == IdRef.class){
            ref = visitIdRef(((IdRef) qr.ref), indent(arg));

            Declaration temp = curClass;
            curClass = ref;
            SId.memberRef = true;
            rhs =  visitIdentifier(qr.id, indent(arg));// think this could be ref
            curClass = temp;

            if(((IdRef) qr.ref).id.kind == TokenType.ID ) {
                if (ref.getClass() == VarDecl.class && ref.type.getClass() == ClassType.class){
                    if ((!(((ClassType) ref.type).className.spelling.equals(curClass.name)))) {
                        if (rhs.getClass() == FieldDecl.class) {
                            if (((FieldDecl) rhs).isPrivate) {

                                throw new IdentificationError(currentTree, "Private");

                            }
                        }
                    }
                }
                else{

                    if (!ref.name.equals(curClass.name)) {
                        if (rhs.getClass() == FieldDecl.class) {
                            if (((FieldDecl) rhs).isPrivate) {

                                throw new IdentificationError(currentTree, "Private");

                            }
                        }
                    }


                }

            }



            if(((IdRef) qr.ref).id.decl.getClass() == ClassDecl.class ){ //i think this is proper but maybe check decl
                if(qr.id.decl.getClass()  == MethodDecl.class){
                    if (!((MethodDecl) qr.id.decl).isStatic){
                        throw new IdentificationError(this.currentTree, "Accessing a nonstatic member from a class");
                    }
                }
                if(qr.id.decl.getClass()  == FieldDecl.class){
                    if (!((FieldDecl) qr.id.decl).isStatic){
                        throw new IdentificationError(this.currentTree, "Accessing a nonstatic member from a class");
                    }

                }
            }


            if (ref.getClass()== MethodDecl.class){
                //return ref;
            }
            SId.memberRef = false;
            return rhs;

        }
        else if(qr.ref.getClass() == ThisRef.class){
            ref = visitThisRef(((ThisRef) qr.ref), indent(arg));
        }
        else{ //ref is a qual ref
            ref = visitQRef(((QualRef) qr.ref), indent(arg));
            if (((QualRef) qr.ref).id.decl.getClass() == MethodDecl.class){
                throw new IdentificationError(this.currentTree, "Using a method as a reference");
            }
            Declaration temp = curClass;
            curClass = ref;
            SId.memberRef = true;
            rhs =  visitIdentifier(qr.id, indent(arg));// think this could be ref
            curClass = temp;


            if(((QualRef) qr.ref).id.kind == TokenType.ID ) {
                if (ref.getClass() == ClassDecl.class &&  !ref.name.equals( curClass.name)) {
                    if (rhs.getClass() == FieldDecl.class) {
                        if (((FieldDecl) rhs).isPrivate) {

                            throw new IdentificationError(currentTree, "Private");

                        }
                    }
                }
                if (ref.getClass() == FieldDecl.class){
                    if(!curClass.name.equals((((ClassType) ref.type).className.spelling))){


                        if (rhs.getClass() == FieldDecl.class) {
                            if (((FieldDecl) rhs).isPrivate) {

                                throw new IdentificationError(currentTree, "Private");

                            }
                        }


                    }
                }
            }
            //ref = qr.id.decl;

            /*
           if(ref.getClass() == ThisRef.class){
               if(qr.id.decl.getClass() == MethodDecl.class){

               }
           }
           else if(ref.getClass() == ThisRef.class){

            }

             */
        }

        Declaration temp = curClass;
        curClass = ref;
        SId.memberRef = true;
        rhs =  visitIdentifier(qr.id, indent(arg));// think this could be ref
        curClass = temp;





        //qr.ref.visit(this, indent(arg));
        SId.memberRef = false;
        return rhs;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Declaration visitIdentifier(Identifier id, String arg){
        Declaration decl = SId.findDeclaration(id, curClass);
        if (decl == null  ){
            throw new IdentificationError(currentTree, "Undeclared value " + id.spelling);
        }

        else{
            id.decl = decl;
        }
        show(arg, quote(id.spelling) + " " + id.toString());
        return decl;
    }

    public Object visitOperator(Operator op, String arg){
        show(arg, quote(op.spelling) + " " + op.toString());
        return null;
    }

    public Object visitIntLiteral(IntLiteral num, String arg){
        show(arg, quote(num.spelling) + " " + num.toString());
        return new BaseType(TypeKind.INT, num.posn);
    }

    public Object visitBooleanLiteral(BooleanLiteral bool, String arg){
        show(arg, quote(bool.spelling) + " " + bool.toString());
        return new BaseType(TypeKind.BOOLEAN, bool.posn);
    }

    public TypeDenoter visitNullLiteral (NullLiteral n1, String arg){
        show(arg, quote(n1.spelling) + " " + n1.toString());
        return new BaseType(TypeKind.NULL, n1.posn);
    }
}




