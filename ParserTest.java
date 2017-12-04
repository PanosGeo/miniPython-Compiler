import java.io.*;
import minipython.lexer.Lexer;
import minipython.parser.Parser;
import minipython.node.*;
import java.util.*;

public class ParserTest
{
  public static void main(String[] args)
  {
    try
    {
      Parser parser =
        new Parser(
        new Lexer(
        new PushbackReader(
        new FileReader(args[0].toString()), 1024)));

     Hashtable vartable =  new Hashtable();
     Hashtable functable = new Hashtable();
     int errors = 0;
     Start ast = parser.parse();
     myvisitor visitor = new myvisitor(vartable, functable);
     ast.apply(visitor);
     errors = visitor.getError();
     if(errors == 0) {
    	 myvisitor2 visitor2 = new myvisitor2(vartable, functable);
    	 ast.apply(visitor2);
         errors = visitor2.getError();
     }
     if(errors != 0) System.out.println("Number of errors found: " + errors);
    }
    catch (Exception e)
    {
      System.err.println(e);
    }
  }
}

