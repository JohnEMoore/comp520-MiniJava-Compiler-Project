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
        ParameterDecl PsParam = new ParameterDecl(new BaseType(TypeKind.INT, defPosn), "x", defPosn);
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
        SId.openScope();  // scope 1
        for (FieldDecl f: clas.fieldDeclList)
            f.visit(this, pfx);
        show(arg,"  MethodDeclList [" + clas.methodDeclList.size() + "]");
        for (MethodDecl m: clas.methodDeclList)
            visitMethodDeclFirst(m);
        for (MethodDecl m: clas.methodDeclList)
            m.visit(this, pfx);
        SId.closeScope();

        return null;
    }

    public Object visitFieldDecl(FieldDecl f, String arg){
        show(arg, "(" + (f.isPrivate ? "private": "public")
                + (f.isStatic ? " static) " :") ") + f.toString());
        f.type.visit(this, indent(arg));
        show(indent(arg), quote(f.name) + " fieldname");
        SId.addDeclaration(curClass + "." + f.name, f);
        return null;
    }

    public Object visitMethodDecl(MethodDecl m, String arg){
        SId.openScope(); // move to scope 2
        show(arg, "(" + (m.isPrivate ? "private": "public")
                + (m.isStatic ? " static) " :") ") + m.toString());
        m.type.visit(this, indent(arg));
        show(indent(arg), quote(m.name) + " methodname");
        ParameterDeclList pdl = m.parameterDeclList;
        show(arg, "  ParameterDeclList [" + pdl.size() + "]");
        String pfx = ((String) arg) + "  . ";

        for (ParameterDecl pd: pdl) {
            pd.visit(this, pfx);
        }
        StatementList sl = m.statementList;
        show(arg, "  StmtList [" + sl.size() + "]");
        for (Statement s: sl) {
            s.visit(this, pfx);
        }
        SId.closeScope();
        return null;
    }

    public Object visitMethodDeclFirst(MethodDecl m){
        SId.addDeclaration(curClass + "." + m.name, m); // me
        StatementList sl = m.statementList;
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

    public Object visitBaseType(BaseType type, String arg){
        show(arg, type.typeKind + " " + type.toString());
        return null;
    }

    public Object visitClassType(ClassType ct, String arg){
        show(arg, ct);
        ct.className.visit(this, indent(arg));
        return null;
    }

    public Object visitArrayType(ArrayType type, String arg){
        show(arg, type);
        type.eltType.visit(this, indent(arg));
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Object visitBlockStmt(BlockStmt stmt, String arg){
        SId.openScope();
        show(arg, stmt);
        StatementList sl = stmt.sl;
        show(arg,"  StatementList [" + sl.size() + "]");
        String pfx = arg + "  . ";
        for (Statement s: sl) {
            s.visit(this, pfx);
        }
        SId.closeScope();
        return null;
    }

    public Object visitVardeclStmt(VarDeclStmt stmt, String arg){

        show(arg, stmt);
        currentVarDecl = stmt.varDecl.name;
        stmt.varDecl.visit(this, indent(arg));
        stmt.initExp.visit(this, indent(arg));
        // put type of var into the indentifier
        currentVarDecl = null;
        return null;
    }

    public Object visitAssignStmt(AssignStmt stmt, String arg){
        show(arg,stmt);
        stmt.ref.visit(this, indent(arg));
        stmt.val.visit(this, indent(arg));
        // the type check goes here
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
        stmt.methodRef.visit(this, indent(arg));

        ExprList al = stmt.argList;
        show(arg,"  ExprList [" + al.size() + "]");
        String pfx = arg + "  . ";
        for (Expression e: al) {
            e.visit(this, pfx);
        }
        return null;
    }

    public Object visitReturnStmt(ReturnStmt stmt, String arg){
        show(arg,stmt);
        if (stmt.returnExpr != null)
            stmt.returnExpr.visit(this, indent(arg));
        return null;
    }

    public Object visitIfStmt(IfStmt stmt, String arg){
        show(arg,stmt);
        stmt.cond.visit(this, indent(arg));
        stmt.thenStmt.visit(this, indent(arg));
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, indent(arg));
        return null;
    }

    public Object visitWhileStmt(WhileStmt stmt, String arg){
        show(arg, stmt);
        stmt.cond.visit(this, indent(arg));
        stmt.body.visit(this, indent(arg));
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Object visitUnaryExpr(UnaryExpr expr, String arg){
        show(arg, expr);
        expr.operator.visit(this, indent(arg));
        expr.expr.visit(this, indent(indent(arg)));
        return null;
    }

    public Object visitBinaryExpr(BinaryExpr expr, String arg){
        show(arg, expr);
        expr.operator.visit(this, indent(arg));
        expr.left.visit(this, indent(indent(arg)));
        expr.right.visit(this, indent(indent(arg)));
        return null;
    }

    public Object visitRefExpr(RefExpr expr, String arg){
        show(arg, expr);
        expr.ref.visit(this, indent(arg));
        return null;
    }

    public Object visitIxExpr(IxExpr ie, String arg){
        show(arg, ie);
        ie.ref.visit(this, indent(arg));
        ie.ixExpr.visit(this, indent(arg));
        return null;
    }

    public Object visitCallExpr(CallExpr expr, String arg){
        show(arg, expr);
        expr.functionRef.visit(this, indent(arg));
        ExprList al = expr.argList;
        show(arg,"  ExprList + [" + al.size() + "]");
        String pfx = arg + "  . ";
        for (Expression e: al) {
            e.visit(this, pfx);
        }
        return null;
    }

    public Object visitLiteralExpr(LiteralExpr expr, String arg){
        show(arg, expr);
        expr.lit.visit(this, indent(arg));
        return null;
    }

    public Object visitNewArrayExpr(NewArrayExpr expr, String arg){
        show(arg, expr);
        expr.eltType.visit(this, indent(arg));
        expr.sizeExpr.visit(this, indent(arg));
        return null;
    }

    public Object visitNewObjectExpr(NewObjectExpr expr, String arg){
        show(arg, expr);
        expr.classtype.visit(this, indent(arg));
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Declaration visitThisRef(ThisRef ref, String arg) {
        show(arg,ref);
        return curClass;
    }

    public Declaration visitIdRef(IdRef ref, String arg) {
        show(arg,ref);
        Declaration decl = visitIdentifier(ref.id, indent(arg));
        if (decl.name.equals(currentVarDecl)){
            throw new IdentificationError(currentTree, "Cant use var in own decl statement");
        }
        return decl;
    }


    public Declaration visitQRef(QualRef qr, String arg) {
        Declaration ref = null;
        Declaration rhs;
        show(arg, qr);
        if (qr.ref.getClass() == IdRef.class){
            ref = visitIdRef(((IdRef) qr.ref), indent(arg));
            if(((IdRef) qr.ref).id.kind == TokenType.CLASS ){ //i think this is proper but maybe check decl
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

        }
        else if(qr.ref.getClass() == ThisRef.class){
            ref = visitThisRef(((ThisRef) qr.ref), indent(arg));
        }
        else{ //ref is a qual ref
           ref = visitQRef(((QualRef) qr.ref), indent(arg));
           if (((QualRef) qr.ref).id.decl.getClass() == MethodDecl.class){
               throw new IdentificationError(this.currentTree, "Using a method as a reference");
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
        rhs =  visitIdentifier(qr.id, indent(arg));// think this could be ref
        curClass = temp;
        //qr.ref.visit(this, indent(arg));
        return rhs;
    }


    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    public Declaration visitIdentifier(Identifier id, String arg){
        Declaration decl = SId.findDeclaration(id, curClass);
        if (decl == null){
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
        return null;
    }

    public Object visitBooleanLiteral(BooleanLiteral bool, String arg){
        show(arg, quote(bool.spelling) + " " + bool.toString());
        return null;
    }

    public Object visitNullLiteral (NullLiteral n1, String arg){
        show(arg, quote(n1.spelling) + " " + n1.toString());
        return null;
    }
}




