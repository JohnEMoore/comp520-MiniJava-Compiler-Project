package miniJava.AbstractSyntaxTrees;

public class IdentificationError extends Error {


    private static final long serialVersionUID = -441346906191470192L;
    private String _errMsg;
    public IdentificationError(AST ast, String errMsg) {
        super();
        this._errMsg = ast.posn == null
        ? "*** " + errMsg
        : "*** " + ast.posn.toString() + ": " + errMsg;
        }



@Override
public String toString() {
        return _errMsg;
        }
        }