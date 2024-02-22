package miniJava.SyntacticAnalyzer;

public class SourcePosition  {
    private int line;

    private int column;


    public SourcePosition(int line, int column){
        this.line = line;
        this.column = column;
    }

    public void addLine(){
        line += 1;
    }

    public void addColumn(){
        column += 1;
    }

    @Override
    public String toString(){
        return "" + this.line + this.column;
    }
}
