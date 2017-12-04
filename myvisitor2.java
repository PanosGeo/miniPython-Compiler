import minipython.analysis.*;
import minipython.node.*;
import java.util.*;

public class myvisitor2 extends DepthFirstAdapter {

	private Hashtable vartable;
	private Hashtable functable;
	private int error, temp, progr_line;
	
	myvisitor2(Hashtable vartable, Hashtable functable) 
	{
		this.vartable = vartable;
		this.functable = functable;
	}
	
	public int getError() {
		return error;
	}
	
	@Override
	public void inAFunctionCallExpression(AFunctionCallExpression node) {
		String func_name = ((AIdentifierExpression) node.getL()).getId().toString().trim();
		int line = ((AIdentifierExpression) node.getL()).getId().getLine();
		int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
		int args = node.getR().size();                                        // gets the number of arguments in the function call
		int error_code = 0, index = 0;
		boolean check = false;                                                 // If check == false, the function is not declared. Checks in the hashtable
		for(int i = 1; i <= functable.size(); i++) {						  // of functions and if at the end, error_code != 0, the same function was found with
			if(func_name.equals( ( (Functions)  functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {   // different parameters
				check = true;
				error_code = 0;
				index = i;
				break;
			}
			if(func_name.equals( ( (Functions)  functable.get(i)).getName() ) && args != ( (Functions) functable.get(i)).getArgs() ) {
				check = true;
				++error_code;
			}
		}
		if(check) {
			if(error_code == 0) {                                          
				ArrayList<String> argtype = new ArrayList<String>();					   // argtype will hold the types that the parameters must match in order to be accepted by the function				
				ArrayList<String> list = new ArrayList<String>();                         // list will hold the names of the function's arguments, which will be checked if they exist in the statement of the function
				String[] types = new String[args];	
				AFunction func =  ((AFunction) ((Functions)  functable.get(index)).getNode());
				for(int i = 0; i < args; i++) {
					list.add( ((Functions) functable.get(index)).getArgName(i) );
					if(func.getStatement() instanceof AAssignmentStatement && ( ((AAssignmentStatement) func.getStatement()).getR() instanceof AAddExpression 
					   || ((AAssignmentStatement) func.getStatement()).getR() instanceof ASubExpression || ((AAssignmentStatement) func.getStatement()).getR() instanceof AMultExpression 
					   ||((AAssignmentStatement) func.getStatement()).getR() instanceof ADivExpression || ((AAssignmentStatement) func.getStatement()).getR() instanceof APlusplusExpression
					   || ((AAssignmentStatement) func.getStatement()).getR() instanceof AMinusminusExpression) ) {
						
						String stat =  ((AAssignmentStatement) func.getStatement()).getR().toString();
						if(stat.contains(list.get(i))) argtype.add("number");   // if the statement contains the argument in the i-nth position, then the parameter in the same position of the call must be a number
						else argtype.add("null");                               // else it can be anything
					}
					else if(func.getStatement() instanceof AMinusEqStatement && ( ((AMinusEqStatement) func.getStatement()).getR() instanceof AAddExpression 
							|| ((AMinusEqStatement) func.getStatement()).getR() instanceof ASubExpression || ((AMinusEqStatement) func.getStatement()).getR() instanceof AMultExpression 
							|| ((AMinusEqStatement) func.getStatement()).getR() instanceof ADivExpression || ((AMinusEqStatement) func.getStatement()).getR() instanceof APlusplusExpression
							|| ((AMinusEqStatement) func.getStatement()).getR() instanceof AMinusminusExpression || ((AMinusEqStatement) func.getStatement()).getR() instanceof AIdentifierExpression) ) {
						
						String id = ((AMinusEqStatement) func.getStatement()).getL().toString();                    // if the statement is a MinusEq, then if the operator is an argument, it has to be a number
						String stat =  ((AMinusEqStatement) func.getStatement()).getR().toString();                // if the right side contains an argument, it must also be a number.
						if(id.contains(list.get(i))) argtype.add("number");										  // if an argument is at both sides, it must be added only once in argstable, as it's size must be equal to the function's arguments
						else if(stat.contains(list.get(i)) && !id.contains(list.get(i))) argtype.add("number");
						else argtype.add("null");                                                                 // if an argument isn't contained in the statement, the function call can have any type
					}
					else if(func.getStatement() instanceof ADivEqStatement && ( ((ADivEqStatement) func.getStatement()).getR() instanceof AAddExpression 
							|| ((ADivEqStatement) func.getStatement()).getR() instanceof ASubExpression || ((ADivEqStatement) func.getStatement()).getR() instanceof AMultExpression 
							|| ((ADivEqStatement) func.getStatement()).getR() instanceof ADivExpression || ((ADivEqStatement) func.getStatement()).getR() instanceof APlusplusExpression
							|| ((ADivEqStatement) func.getStatement()).getR() instanceof AMinusminusExpression || ((ADivEqStatement) func.getStatement()).getR() instanceof AIdentifierExpression) ) {
						
						String id = ((ADivEqStatement) func.getStatement()).getL().toString();                        // same as MinusEq
						String stat =  ((ADivEqStatement) func.getStatement()).getR().toString();
						if(id.contains(list.get(i))) argtype.add("number");
						else if(stat.contains(list.get(i)) && !id.contains(list.get(i))) argtype.add("number");
						else argtype.add("null");
					}
					else if(func.getStatement() instanceof AReturnExprStatement && ( ((AReturnExprStatement) func.getStatement()).getExpression() instanceof AAddExpression 
							|| ((AReturnExprStatement) func.getStatement()).getExpression() instanceof ASubExpression || ((AReturnExprStatement) func.getStatement()).getExpression() instanceof AMultExpression 
							|| ((AReturnExprStatement) func.getStatement()).getExpression() instanceof ADivExpression || ((AReturnExprStatement) func.getStatement()).getExpression() instanceof APlusplusExpression
							|| ((AReturnExprStatement) func.getStatement()).getExpression() instanceof AMinusminusExpression) ) {
						
						String stat =  ((AReturnExprStatement) func.getStatement()).getExpression().toString();         // if the return statement has arithmetic operations and contains
						if(stat.contains(list.get(i))) argtype.add("number");											// an argument, the argument must be a number, else it can be anything
						else argtype.add("null");
					}
					else if(func.getStatement() instanceof APrintExprStatement) {
						String stat = "";
						if( ((APrintExprStatement) func.getStatement()).getL() instanceof AAddExpression || ((APrintExprStatement) func.getStatement()).getL() instanceof ASubExpression
							|| ((APrintExprStatement) func.getStatement()).getL() instanceof AMultExpression || ((APrintExprStatement) func.getStatement()).getL() instanceof ADivExpression 
							|| ((APrintExprStatement) func.getStatement()).getL() instanceof APlusplusExpression || ((APrintExprStatement) func.getStatement()).getL() instanceof AMinusminusExpression) {
							
							stat =  ((APrintExprStatement) func.getStatement()).getL().toString();                    // A print statement might have more than one expressions. So for each statement
							if(stat.contains(list.get(i))) types[i] = "number";										 // we want to check if it contains an argument. If the first expression
 							else types[i] = "null";																	// is an arithmetic operation and contains an argument, the argument must be a number
						}																						   // else it can be anything
						if( ((APrintExprStatement) func.getStatement()).getR().size() > 0 ) {						// if there are more expressions, we check them as well
							for(int j = 0; j < ((APrintExprStatement) func.getStatement()).getR().size(); j++) {
								if( ((APrintExprStatement) func.getStatement()).getR().get(j) instanceof AAddExpression || ((APrintExprStatement) func.getStatement()).getR().get(j) instanceof ASubExpression
									|| ((APrintExprStatement) func.getStatement()).getR().get(j) instanceof AMultExpression || ((APrintExprStatement) func.getStatement()).getR().get(j) instanceof ADivExpression 
								 	|| ((APrintExprStatement) func.getStatement()).getR().get(j) instanceof APlusplusExpression 
								 	|| ((APrintExprStatement) func.getStatement()).getR().get(j) instanceof AMinusminusExpression) {
								
									String stat2 =  ((APrintExprStatement) func.getStatement()).getR().get(j).toString();             // if an argument is contained in a previous one, it's type has been set
									if(stat2.contains(list.get(i)) && !types[i].equals("number")) types[i] = "number";				 // and must not be changed again or added in the list. For that we use the array
									else if(!stat.contains(list.get(i)) && !types[i].equals("number")) types[i] = "null";			// types[] to temporarily hold the types. If an argument is contained in an expression
								}																								   // and has been declared a number by a previous one, it's type doesn't change. If no expression
							}																									  // contains an argument, then it can be anything
						} 
					}
				}
				if(func.getStatement() instanceof APrintExprStatement) {   // if the statement is a print, we transfer the types to argtype
					for(int i = 0; i < args; i++) {
						argtype.add(types[i]);
					}
				}
				for(int i = 0; i < node.getR().size(); i++) {
					if(node.getR().get(i) instanceof AStringExpression && argtype.get(i).equals("number")) {         // then we use argtype to check if the parameters
						String name = ( (AStringExpression) node.getR().get(i)).getString().toString().trim();		// in the fuction call can be used
						line = ( (AStringExpression) node.getR().get(i)).getString().getLine();
						pos = ( (AStringExpression) node.getR().get(i)).getString().getPos();
						System.out.println("[" + line + "," + pos + "]: " + "Argument " + name + " not applicable for this function.");
						++error;
					}
					else if(node.getR().get(i) instanceof AIdentifierExpression) {
						String type = CheckVariableDeclaration((AIdentifierExpression) node.getR().get(i));
						if(type.equals("string") && argtype.get(i).equals("number")) {
							String name = ((AIdentifierExpression) node.getR().get(i)).getId().toString().trim();
							line = ((AIdentifierExpression) node.getR().get(i)).getId().getLine();
							pos = ((AIdentifierExpression) node.getR().get(i)).getId().getPos();
							System.out.println("[" + line + "," + pos + "]: " + "Argument " + name + " not applicable for this function.");
							++error;
						}
					}
				}
			}
			else {
				System.out.println("[" + line + "," + pos + "]" + ": Invalid number of parameters for function " + func_name + ".");
				++error;
			}
		}
		else {
			System.out.println("[" + line + "," + pos + "]" + ": Function " + func_name + " is not defined.");
			++error;
		}
	}
	
	public String CheckVariableDeclaration (AIdentifierExpression node) {
		String name = node.getId().toString().trim();
		int line = node.getId().getLine();
		int pos = node.getId().getPos();
		String type = "";
		boolean check = false;
		for(int i = 1; i <= vartable.size(); i++) {
			if( ( (Variable) vartable.get(i)).getName().equals(name)) {
				check = true;
				type = ((Variable) vartable.get(i)).getType();
			}
		}
		if(!check) {
			System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " isn't declared.");
			++error;
		}
		return type;
	}
	
	@Override
	public void inAAssignmentStatement(AAssignmentStatement node) {
		temp = error;
	}
	
	@Override
	public void outAAssignmentStatement(AAssignmentStatement node) {
		++progr_line;
		if(temp == error) {
			String varname = ((AIdentifierExpression) node.getL()).getId().toString().trim();  // Method getL() of AAsignmentStatement gets the identifier before the = operator
			String vartype = "";
			if(node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression || node.getR() instanceof ADivExpression
				|| node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression || node.getR() instanceof ANumberExpression) {
				vartype = "number";
			}
			else if(node.getR() instanceof AStringExpression) vartype = "string";
			else if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression)node.getR());
				if(!type.isEmpty()) vartype = type;
			}
			else if(node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
			}
			for(int i = 1; i <= vartable.size(); i++) {                  			   // Checks if the variable is already declared. If it is,
				if( ( (Variable)vartable.get(i)).getName().equals(varname)) {         // then changes the type of the variable to the one on the left of 
					( (Variable) vartable.get(i)).setType(vartype);					 // the = operator
				}
			}
		}
	}
	
	@Override
	public void inAAddExpression(AAddExpression node) {
		boolean check = true;
		temp = error;
		if(node.getL() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getL()).getString().toString().trim();
			int line = ((AStringExpression) node.getL()).getString().getLine();
			int pos = ((AStringExpression) node.getL()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
			check = false;
		}
		if(node.getL() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getL());
			if(!type.isEmpty() && type.equals("string")) {
				String name = ((AIdentifierExpression) node.getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getL()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof ALeftbrIdentifierExpression) {
			int line = 0, pos = 0;
			if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof ANumberExpression ) {
				line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getLine();
				pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			else if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof AStringExpression ) {
				line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getLine();
				pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof AIdentifierLeftbrExpression) {
			String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().toString().trim();
			name = name.concat("[]");
			String type = "";
			int key = 0;
			if( ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ANumberExpression) {
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getNum().toString().trim() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AIdentifierExpression) {
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getId().toString().trim();
				int index = 0;
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
				}
				key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ASubExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ADivExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMinusminusExpression) {
				key = getResult( ((AIdentifierLeftbrExpression) node.getL()).getR() );
			}
			for(int i = 1; i <= vartable.size(); i++) {
				if( ((Variable) vartable.get(i)).getName().equals(name) ) {
					type = ((Variable) vartable.get(i)).getElement(key);
				}
			}
			if(!type.equals("number")) {
				int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(check) {
			if(node.getR() instanceof AStringExpression) {
				String name = ((AStringExpression) node.getR()).getString().toString().trim();
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
				++error;
			}
			if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(!type.isEmpty() && type.equals("string")) {
					String name = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
					++error;
				}
			}
			if(node.getR() instanceof ALeftbrIdentifierExpression) {
				int line = 0, pos = 0;
				if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof ANumberExpression ) {
					line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getLine();
					pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
				}
				else if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof AStringExpression ) {
					line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getLine();
					pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
				}
			}
			if(node.getR() instanceof AIdentifierLeftbrExpression) {
				String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().toString().trim();
				name = name.concat("[]");
				String type = "";
				int key = 0;
				if( ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ANumberExpression) {
					key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getNum().toString().trim() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AIdentifierExpression) {
					name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getId().toString().trim();
					int index = 0;
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
					}
					key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ASubExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ADivExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMinusminusExpression) {
					key = getResult( ((AIdentifierLeftbrExpression) node.getR()).getR() );
				}
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) {
						type = ((Variable) vartable.get(i)).getElement(key);
					}
				}
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getLine();
					int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
					++error;
				}
			}
		}
	}
	
	@Override
	public void outAAddExpression(AAddExpression node) {
		if(temp == error) {
			if(node.getL() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getL()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
			if(node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
		}
	}
	
	@Override
	public void inASubExpression(ASubExpression node) {
		boolean check = true;
		temp = error;
		if(node.getL() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getL()).getString().toString().trim();
			int line = ((AStringExpression) node.getL()).getString().getLine();
			int pos = ((AStringExpression) node.getL()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
			check = false;
		}
		if(node.getL() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getL());
			if(!type.isEmpty() && type.equals("string")) {
				String name = ((AIdentifierExpression) node.getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getL()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof ALeftbrIdentifierExpression) {
			int line = 0, pos = 0;
			if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof ANumberExpression ) {
				line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getLine();
				pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			else if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof AStringExpression ) {
				line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getLine();
				pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof AIdentifierLeftbrExpression) {
			String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().toString().trim();
			name = name.concat("[]");
			String type = "";
			int key = 0;
			if( ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ANumberExpression) {
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getNum().toString().trim() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AIdentifierExpression) {
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getId().toString().trim();
				int index = 0;
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
				}
				key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ASubExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ADivExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMinusminusExpression) {
				key = getResult( ((AIdentifierLeftbrExpression) node.getL()).getR() );
			}
			for(int i = 1; i <= vartable.size(); i++) {
				if( ((Variable) vartable.get(i)).getName().equals(name) ) {
					type = ((Variable) vartable.get(i)).getElement(key);
				}
			}
			if(!type.equals("number")) {
				int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(check) {
			if(node.getR() instanceof AStringExpression) {
				String name = ((AStringExpression) node.getR()).getString().toString().trim();
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(!type.isEmpty() && type.equals("string")) {
					String name = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
					++error;
					check = false;
				}
			}
			if(node.getR() instanceof ALeftbrIdentifierExpression) {
				int line = 0, pos = 0;
				if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof ANumberExpression ) {
					line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getLine();
					pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
					check = false;
				}
				else if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof AStringExpression ) {
					line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getLine();
					pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
					check = false;
				}
			}
			if(node.getR() instanceof AIdentifierLeftbrExpression) {
				String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().toString().trim();
				name = name.concat("[]");
				String type = "";
				int key = 0;
				if( ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ANumberExpression) {
					key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getNum().toString().trim() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AIdentifierExpression) {
					name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getId().toString().trim();
					int index = 0;
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
					}
					key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ASubExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ADivExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMinusminusExpression) {
					key = getResult( ((AIdentifierLeftbrExpression) node.getR()).getR() );
				}
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) {
						type = ((Variable) vartable.get(i)).getElement(key);
					}
				}
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getLine();
					int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
					++error;
				}
			}
			
		}
	}
	
	@Override
	public void outASubExpression(ASubExpression node) {
		if(temp == error) {
			if(node.getL() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getL()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
			if(node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
		}
	}
	
	@Override
	public void inAMultExpression(AMultExpression node) {
		boolean check = true;
		temp = error;
		if(node.getL() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getL()).getString().toString().trim();
			int line = ((AStringExpression) node.getL()).getString().getLine();
			int pos = ((AStringExpression) node.getL()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
			check = false;
		}
		if(node.getL() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getL());
			if(!type.isEmpty() && type.equals("string")) {
				String name = ((AIdentifierExpression) node.getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getL()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof ALeftbrIdentifierExpression) {
			int line = 0, pos = 0;
			if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof ANumberExpression ) {
				line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getLine();
				pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			else if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof AStringExpression ) {
				line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getLine();
				pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof AIdentifierLeftbrExpression) {
			String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().toString().trim();
			name = name.concat("[]");
			String type = "";
			int key = 0;
			if( ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ANumberExpression) {
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getNum().toString().trim() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AIdentifierExpression) {
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getId().toString().trim();
				int index = 0;
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
				}
				key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ASubExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ADivExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMinusminusExpression) {
				key = getResult( ((AIdentifierLeftbrExpression) node.getL()).getR() );
			}
			for(int i = 1; i <= vartable.size(); i++) {
				if( ((Variable) vartable.get(i)).getName().equals(name) ) {
					type = ((Variable) vartable.get(i)).getElement(key);
				}
			}
			if(!type.equals("number")) {
				int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(check) {
			if(node.getR() instanceof AStringExpression) {
				String name = ((AStringExpression) node.getR()).getString().toString().trim();
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(!type.isEmpty() && type.equals("string")) {
					String name = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
					++error;
					check = false;
				}
			}
			if(node.getR() instanceof ALeftbrIdentifierExpression) {
				int line = 0, pos = 0;
				if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof ANumberExpression ) {
					line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getLine();
					pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
					check = false;
				}
				else if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof AStringExpression ) {
					line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getLine();
					pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
					check = false;
				}
			}
			if(node.getR() instanceof AIdentifierLeftbrExpression) {
				String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().toString().trim();
				name = name.concat("[]");
				String type = "";
				int key = 0;
				if( ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ANumberExpression) {
					key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getNum().toString().trim() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AIdentifierExpression) {
					name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getId().toString().trim();
					int index = 0;
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
					}
					key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ASubExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ADivExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMinusminusExpression) {
					key = getResult( ((AIdentifierLeftbrExpression) node.getR()).getR() );
				}
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) {
						type = ((Variable) vartable.get(i)).getElement(key);
					}
				}
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getLine();
					int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
					++error;
				}
			}
		}
	}
	
	@Override
	public void outAMultExpression(AMultExpression node) {
		if(temp == error) {
			if(node.getL() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getL()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
			if(node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
		}
	}
	
	@Override
	public void inADivExpression(ADivExpression node) {
		boolean check = true;
		temp = error;
		if(node.getL() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getL()).getString().toString().trim();
			int line = ((AStringExpression) node.getL()).getString().getLine();
			int pos = ((AStringExpression) node.getL()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
			check = false;
		}
		if(node.getL() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getL());
			if(!type.isEmpty() && type.equals("string")) {
				String name = ((AIdentifierExpression) node.getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getL()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof ALeftbrIdentifierExpression) {
			int line = 0, pos = 0;
			if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof ANumberExpression ) {
				line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getLine();
				pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			else if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof AStringExpression ) {
				line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getLine();
				pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof AIdentifierLeftbrExpression) {
			String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().toString().trim();
			name = name.concat("[]");
			String type = "";
			int key = 0;
			if( ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ANumberExpression) {
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getNum().toString().trim() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AIdentifierExpression) {
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getId().toString().trim();
				int index = 0;
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
				}
				key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
			}
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ASubExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ADivExpression 
					|| ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AMinusminusExpression) {
				key = getResult( ((AIdentifierLeftbrExpression) node.getL()).getR() );
			}
			for(int i = 1; i <= vartable.size(); i++) {
				if( ((Variable) vartable.get(i)).getName().equals(name) ) {
					type = ((Variable) vartable.get(i)).getElement(key);
				}
			}
			if(!type.equals("number")) {
				int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(check) {
			if(node.getR() instanceof AStringExpression) {
				String name = ((AStringExpression) node.getR()).getString().toString().trim();
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
			if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(!type.isEmpty() && type.equals("string")) {
					String name = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
					++error;
					check = false;
				}
			}
			if(node.getR() instanceof ALeftbrIdentifierExpression) {
				int line = 0, pos = 0;
				if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof ANumberExpression ) {
					line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getLine();
					pos = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getNum().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
					check = false;
				}
				else if( ((ALeftbrIdentifierExpression) node.getR()).getL() instanceof AStringExpression ) {
					line = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getLine();
					pos = ( (AStringExpression) ((ALeftbrIdentifierExpression) node.getR()).getL()).getString().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used in arithmetic operations.");
					++error;
					check = false;
				}
			}
			if(node.getR() instanceof AIdentifierLeftbrExpression) {
				String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().toString().trim();
				name = name.concat("[]");
				String type = "";
				int key = 0;
				if( ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ANumberExpression) {
					key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getNum().toString().trim() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AIdentifierExpression) {
					name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getR()).getId().toString().trim();
					int index = 0;
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
					}
					key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
				}
				else if(((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ASubExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof ADivExpression 
						|| ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getR()).getR() instanceof AMinusminusExpression) {
					key = getResult( ((AIdentifierLeftbrExpression) node.getR()).getR() );
				}
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) {
						type = ((Variable) vartable.get(i)).getElement(key);
					}
				}
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getLine();
					int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getR()).getL()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
					++error;
				}
			}
		}
	}
	
	@Override
	public void outADivExpression(ADivExpression node) {
		if(temp == error) {
			if(node.getL() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getL()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getL()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
			if(node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
		}
	}
	
	@Override
	public void inAPlusplusExpression(APlusplusExpression node) {
		temp = error;
		if(node.getExpression() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getExpression()).getString().toString().trim();
			int line = ((AStringExpression) node.getExpression()).getString().getLine();
			int pos = ((AStringExpression) node.getExpression()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
		}
		if(node.getExpression() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getExpression());
			if(!type.isEmpty() && type.equals("string")) {
				String name = ((AIdentifierExpression) node.getExpression()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getExpression()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getExpression()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
			}
		}
		if(node.getExpression() instanceof AIdentifierLeftbrExpression) {
			String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getL()).getId().toString().trim();
			name = name.concat("[]");
			String type = "";
			int key = 0;
			if( ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof ANumberExpression) {
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getR()).getNum().toString().trim() );
			}
			else if(((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AIdentifierExpression) {
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getR()).getId().toString().trim();
				int index = 0;
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
				}
				key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
			}
			else if(((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof ASubExpression 
					|| ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof ADivExpression 
					|| ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AMinusminusExpression) {
				key = getResult( ((AIdentifierLeftbrExpression) node.getExpression()).getR() );
			}
			for(int i = 1; i <= vartable.size(); i++) {
				if( ((Variable) vartable.get(i)).getName().equals(name) ) {
					type = ((Variable) vartable.get(i)).getElement(key);
				}
			}
			if(!type.equals("number")) {
				int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
				++error;
			}
		}
	}
	
	@Override
	public void outAPlusplusExpression(APlusplusExpression node) {
		if(temp == error) {
			if(node.getExpression() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
		}
	}
	
	@Override
	public void inAMinusminusExpression(AMinusminusExpression node) {
		temp = error;
		if(node.getExpression() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getExpression()).getString().toString().trim();
			int line = ((AStringExpression) node.getExpression()).getString().getLine();
			int pos = ((AStringExpression) node.getExpression()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
		}
		if(node.getExpression() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getExpression());
			if(!type.isEmpty() && type.equals("string")) {
				String name = ((AIdentifierExpression) node.getExpression()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getExpression()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getExpression()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
			}
		}
		if(node.getExpression() instanceof AIdentifierLeftbrExpression) {
			String name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getL()).getId().toString().trim();
			name = name.concat("[]");
			String type = "";
			int key = 0;
			if( ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof ANumberExpression) {
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getR()).getNum().toString().trim() );
			}
			else if(((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AIdentifierExpression) {
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getR()).getId().toString().trim();
				int index = 0;
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
				}
				key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
			}
			else if(((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AAddExpression || ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof ASubExpression 
					|| ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AMultExpression || ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof ADivExpression 
					|| ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof APlusplusExpression || ((AIdentifierLeftbrExpression) node.getExpression()).getR() instanceof AMinusminusExpression) {
				key = getResult( ((AIdentifierLeftbrExpression) node.getExpression()).getR() );
			}
			for(int i = 1; i <= vartable.size(); i++) {
				if( ((Variable) vartable.get(i)).getName().equals(name) ) {
					type = ((Variable) vartable.get(i)).getElement(key);
				}
			}
			if(!type.equals("number")) {
				int line = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getExpression()).getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Array " + name + " contains string and be used in arithmetic operations.");
				++error;
			}
		}
	}
	
	@Override
	public void outAMinusminusExpression(AMinusminusExpression node) {
		if(temp == error) {
			if(node.getExpression() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) node.getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) node.getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) node.getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]" + ": Invalid call of void function " + name);
					++error;
				}
				else if(type.equals("string")) {
					System.out.println("[" + line + "," + pos + "]" + ": Function " + name + " returns a string and can't be used in arithmetic operations.");
				}
			}
		}
	}
	
	@Override
	public void inALessComparison(ALessComparison node) {
		temp = error;
	}
	
	@Override
	public void outALessComparison(ALessComparison node) {
		if(temp == error) {
			if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
				((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
				) && node.getR() instanceof AStringExpression ) {
				
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && (node.getR() instanceof ANumberExpression 
					|| node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression
					|| node.getR() instanceof ADivExpression || node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) ) {
				
				int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
				int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && node.getR() instanceof AStringExpression ) {
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("string")) {
					int line = ((AStringExpression) node.getR()).getString().getLine();
					int pos = ((AStringExpression) node.getR()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && (node.getR() instanceof ANumberExpression 
					|| node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression
					|| node.getR() instanceof ADivExpression || node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getLine() ;
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && node.getR() instanceof AIdentifierExpression ) {
				String type = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type.equals("string")) {
					int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
					int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && node.getR() instanceof AIdentifierExpression ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && node.getR() instanceof AIdentifierExpression) {
				String type1 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type1.equals(type2)) {
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && node.getR() instanceof AStringExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && (node.getR() instanceof ANumberExpression 
					|| node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression
					|| node.getR() instanceof ADivExpression || node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) ) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && node.getR() instanceof AIdentifierExpression ) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}	
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression)  node.getR()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression)  node.getR()).getR().size() ;
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && node.getR() instanceof AFunctionCallExpression) {
				
				String name = ((AIdentifierExpression)  node.getR()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression)  node.getR()).getR().size() ;
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison)node.getL()).getExpression() instanceof AIdentifierExpression && node.getR() instanceof AFunctionCallExpression) {
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if(((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && node.getR() instanceof AFunctionCallExpression) {
				
				String name1 = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args1 = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name1.equals( ( (Functions) functable.get(i)).getName() ) && args1 == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type1.equals("void")) {
					String name2 = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim() ;
					line = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
					pos = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
					int args2 = ((AFunctionCallExpression) node.getR()).getR().size();
					String type2 = "";
					for(int i = 1; i <= functable.size(); i++) {
						if(name2.equals( ( (Functions) functable.get(i)).getName() ) && args2 == ( (Functions) functable.get(i)).getArgs() ) {
							type2 = ((Functions) functable.get(i)).getReturnType();
						}
					}
					if(!type2.equals("void")) {
						if(!type1.equals(type2)) {
							System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
							++error;
						}
					}
					else {
						System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name2);
						++error;
					}
				}
				else {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name1);
					++error;
				}
			}
		}
	}
	
	@Override
	public void inAGreaterComparison(AGreaterComparison node) {
		temp = error;
	}
	
	@Override
	public void outAGreaterComparison(AGreaterComparison node) {
		if(temp == error) {
			if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
				((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
				) && node.getR() instanceof AStringExpression ) {
				
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && (node.getR() instanceof ANumberExpression 
					|| node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression
					|| node.getR() instanceof ADivExpression || node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) ) {
				
				int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
				int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && node.getR() instanceof AStringExpression ) {
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("string")) {
					int line = ((AStringExpression) node.getR()).getString().getLine();
					int pos = ((AStringExpression) node.getR()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && (node.getR() instanceof ANumberExpression 
					|| node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression
					|| node.getR() instanceof ADivExpression || node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getLine() ;
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && node.getR() instanceof AIdentifierExpression ) {
				String type = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type.equals("string")) {
					int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
					int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && node.getR() instanceof AIdentifierExpression ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && node.getR() instanceof AIdentifierExpression) {
				String type1 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type1.equals(type2)) {
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && node.getR() instanceof AStringExpression) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && (node.getR() instanceof ANumberExpression 
					|| node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression
					|| node.getR() instanceof ADivExpression || node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) ) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && node.getR() instanceof AIdentifierExpression ) {
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) node.getR()) );
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}	
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && node.getR() instanceof AFunctionCallExpression) {
				String name = ((AIdentifierExpression)  node.getR()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression)  node.getR()).getR().size() ;
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && node.getR() instanceof AFunctionCallExpression) {
				
				String name = ((AIdentifierExpression)  node.getR()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression)  node.getR()).getR().size() ;
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison)node.getL()).getExpression() instanceof AIdentifierExpression && node.getR() instanceof AFunctionCallExpression) {
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) node.getR()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if(((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && node.getR() instanceof AFunctionCallExpression) {
				
				String name1 = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args1 = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name1.equals( ( (Functions) functable.get(i)).getName() ) && args1 == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type1.equals("void")) {
					String name2 = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().toString().trim() ;
					line = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getLine();
					pos = ( (AIdentifierExpression) ((AFunctionCallExpression) node.getR()).getL()).getId().getPos();
					int args2 = ((AFunctionCallExpression) node.getR()).getR().size();
					String type2 = "";
					for(int i = 1; i <= functable.size(); i++) {
						if(name2.equals( ( (Functions) functable.get(i)).getName() ) && args2 == ( (Functions) functable.get(i)).getArgs() ) {
							type2 = ((Functions) functable.get(i)).getReturnType();
						}
					}
					if(!type2.equals("void")) {
						if(!type1.equals(type2)) {
							System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
							++error;
						}
					}
					else {
						System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name2);
						++error;
					}
				}
				else {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name1);
					++error;
				}
			}
		}
	}
	
	@Override
	public void inAEqualComparison(AEqualComparison node) {
		temp = error;
	}
	
	@Override
	public void outAEqualComparison(AEqualComparison node) {
		if(temp == error) {
			if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
				((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
				) && ((APlmComparison) node.getR()).getExpression() instanceof AStringExpression ) {
				
				int line = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getLine();
				int pos = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && (((APlmComparison) node.getR()).getExpression() instanceof ANumberExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AAddExpression || ((APlmComparison) node.getR()).getExpression() instanceof ASubExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AMultExpression || ((APlmComparison) node.getR()).getExpression() instanceof ADivExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof APlusplusExpression || ((APlmComparison) node.getR()).getExpression() instanceof AMinusminusExpression) ) {
				
				int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
				int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && ((APlmComparison) node.getR()).getExpression() instanceof AStringExpression ) {
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("string")) {
					int line = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getLine();
					int pos = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && (((APlmComparison) node.getR()).getExpression() instanceof ANumberExpression 
					||((APlmComparison) node.getR()).getExpression() instanceof AAddExpression || ((APlmComparison) node.getR()).getExpression() instanceof ASubExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AMultExpression || ((APlmComparison) node.getR()).getExpression() instanceof ADivExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof APlusplusExpression || ((APlmComparison) node.getR()).getExpression() instanceof AMinusminusExpression) ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getLine() ;
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()) );
				if(!type.equals("string")) {
					int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
					int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression() ) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getLine();
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression) {
				String type1 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()) );
				if(!type1.equals(type2)) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getLine();
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && ((APlmComparison) node.getR()).getExpression() instanceof AStringExpression) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && (((APlmComparison) node.getR()).getExpression() instanceof ANumberExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AAddExpression || ((APlmComparison) node.getR()).getExpression() instanceof ASubExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AMultExpression || ((APlmComparison) node.getR()).getExpression() instanceof ADivExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof APlusplusExpression || ((APlmComparison) node.getR()).getExpression() instanceof AMinusminusExpression) ) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression ) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()));
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}	
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();;
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison)node.getL()).getExpression() instanceof AIdentifierExpression && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if(((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression 
					 && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name1 = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args1 = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name1.equals( ( (Functions) functable.get(i)).getName() ) && args1 == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type1.equals("void")) {
					String name2 = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
					line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
					pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
					int args2 = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();
					String type2 = "";
					for(int i = 1; i <= functable.size(); i++) {
						if(name2.equals( ( (Functions) functable.get(i)).getName() ) && args2 == ( (Functions) functable.get(i)).getArgs() ) {
							type2 = ((Functions) functable.get(i)).getReturnType();
						}
					}
					if(!type2.equals("void")) {
						if(!type1.equals(type2)) {
							System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
							++error;
						}
					}
					else {
						System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name2);
						++error;
					}
				}
				else {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name1);
					++error;
				}
			}
		}
	}
	
	@Override
	public void inANotEqualComparison(ANotEqualComparison node) {
		temp = error;
	}
	
	@Override
	public void outANotEqualComparison(ANotEqualComparison node) {
		if(temp == error) {
			if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
				((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
				|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
				) && ((APlmComparison) node.getR()).getExpression() instanceof AStringExpression ) {
				
				int line = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getLine();
				int pos = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && (((APlmComparison) node.getR()).getExpression() instanceof ANumberExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AAddExpression || ((APlmComparison) node.getR()).getExpression() instanceof ASubExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AMultExpression || ((APlmComparison) node.getR()).getExpression() instanceof ADivExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof APlusplusExpression || ((APlmComparison) node.getR()).getExpression() instanceof AMinusminusExpression) ) {
				
				int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
				int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
				System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
				++error;
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && ((APlmComparison) node.getR()).getExpression() instanceof AStringExpression ) {
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("string")) {
					int line = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getLine();
					int pos = ((AStringExpression) ((APlmComparison) node.getR()).getExpression()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && (((APlmComparison) node.getR()).getExpression() instanceof ANumberExpression 
					||((APlmComparison) node.getR()).getExpression() instanceof AAddExpression || ((APlmComparison) node.getR()).getExpression() instanceof ASubExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AMultExpression || ((APlmComparison) node.getR()).getExpression() instanceof ADivExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof APlusplusExpression || ((APlmComparison) node.getR()).getExpression() instanceof AMinusminusExpression) ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getLine() ;
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()) );
				if(!type.equals("string")) {
					int line =  ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getLine();
					int pos = ((AStringExpression) ((APlmComparison) node.getL()).getExpression()).getString().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression ) {
				
				String type = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression() ) );
				if(!type.equals("number")) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getLine();
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AIdentifierExpression && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression) {
				String type1 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()) );
				if(!type1.equals(type2)) {
					int line = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getLine();
					int pos = ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()).getId().getPos();
					System.out.println("[" + line + "," + pos +  "]: Comparison type missmatch.");
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && ((APlmComparison) node.getR()).getExpression() instanceof AStringExpression) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && (((APlmComparison) node.getR()).getExpression() instanceof ANumberExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AAddExpression || ((APlmComparison) node.getR()).getExpression() instanceof ASubExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof AMultExpression || ((APlmComparison) node.getR()).getExpression() instanceof ADivExpression 
					|| ((APlmComparison) node.getR()).getExpression() instanceof APlusplusExpression || ((APlmComparison) node.getR()).getExpression() instanceof AMinusminusExpression) ) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression && ((APlmComparison) node.getR()).getExpression() instanceof AIdentifierExpression ) {
				
				String name = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim();
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getR()).getExpression()));
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}	
			}
			else if( ((APlmComparison) node.getL()).getExpression() instanceof AStringExpression && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();;
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("string") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ( ((APlmComparison)node.getL()).getExpression() instanceof ANumberExpression || 
					((APlmComparison)node.getL()).getExpression() instanceof AAddExpression  || ((APlmComparison)node.getL()).getExpression() instanceof ASubExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof AMultExpression || ((APlmComparison)node.getL()).getExpression() instanceof ADivExpression
					|| ((APlmComparison)node.getL()).getExpression() instanceof APlusplusExpression || ((APlmComparison)node.getL()).getExpression() instanceof AMinusminusExpression
					) && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
				int line = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
				int pos = ((AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();
				String type = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type.equals("number") && !type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if( ((APlmComparison)node.getL()).getExpression() instanceof AIdentifierExpression && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
				int args = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name.equals( ( (Functions) functable.get(i)).getName() ) && args == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				String type2 = CheckVariableDeclaration( ((AIdentifierExpression) ((APlmComparison) node.getL()).getExpression()) );
				if(!type1.equals(type2) && !type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
					++error;
				}
				else if(type1.equals("void")) {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name);
					++error;
				}
			}
			else if(((APlmComparison) node.getL()).getExpression() instanceof AFunctionCallExpression 
					 && ((APlmComparison) node.getR()).getExpression() instanceof AFunctionCallExpression) {
				
				String name1 = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().toString().trim() ;
				int line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getLine();
				int pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getL()).getId().getPos();
				int args1 = ((AFunctionCallExpression) ((APlmComparison) node.getL()).getExpression()).getR().size();
				String type1 = "";
				for(int i = 1; i <= functable.size(); i++) {
					if(name1.equals( ( (Functions) functable.get(i)).getName() ) && args1 == ( (Functions) functable.get(i)).getArgs() ) {
						type1 = ((Functions) functable.get(i)).getReturnType();
					}
				}
				if(!type1.equals("void")) {
					String name2 = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().toString().trim() ;
					line = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getLine();
					pos = ( (AIdentifierExpression) ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getL()).getId().getPos();
					int args2 = ((AFunctionCallExpression) ((APlmComparison) node.getR()).getExpression()).getR().size();
					String type2 = "";
					for(int i = 1; i <= functable.size(); i++) {
						if(name2.equals( ( (Functions) functable.get(i)).getName() ) && args2 == ( (Functions) functable.get(i)).getArgs() ) {
							type2 = ((Functions) functable.get(i)).getReturnType();
						}
					}
					if(!type2.equals("void")) {
						if(!type1.equals(type2)) {
							System.out.println("[" + line + "," + pos + "]:" + " Comparison type missmatch.");
							++error;
						}
					}
					else {
						System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name2);
						++error;
					}
				}
				else {
					System.out.println("[" + line + "," + pos + "]:" + " Invalid call of void function " + name1);
					++error;
				}
			}
		}
	}
	
	private int getResult(PExpression node) {
		int result = 0;
		if(node instanceof AAddExpression) {
			if(((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof ANumberExpression) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression)((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ANumberExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AAddExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b = 0;
				String name1 = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				String name2 = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name1) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
					if( ((Variable) vartable.get(i)).getName().equals(name2) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((AAddExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((AAddExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((AAddExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((AAddExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AIdentifierExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((AAddExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((AAddExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((AAddExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((AAddExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AAddExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				int b = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AAddExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				int b = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				int b = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ASubExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMultExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof ADivExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ADivExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof APlusplusExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (APlusplusExpression) ((AAddExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
			else if( ((AAddExpression) node).getL() instanceof AMinusminusExpression && ((AAddExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMinusminusExpression) ((AAddExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AAddExpression) node).getL() );
				result = a + b;
			}
		}
		else if(node instanceof ASubExpression) {
			if(((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof ANumberExpression) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression)((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ANumberExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ASubExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b = 0;
				String name1 = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				String name2 = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name1) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
					if( ((Variable) vartable.get(i)).getName().equals(name2) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((ASubExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((ASubExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((ASubExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((ASubExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AIdentifierExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((ASubExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((ASubExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((ASubExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((ASubExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ASubExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				int b = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AAddExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				int b = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				int b = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ASubExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMultExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof ADivExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ADivExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof APlusplusExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (APlusplusExpression) ((ASubExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
			else if( ((ASubExpression) node).getL() instanceof AMinusminusExpression && ((ASubExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMinusminusExpression) ((ASubExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ASubExpression) node).getL() );
				result = Math.abs(a - b);
			}
		}
		else if(node instanceof AMultExpression) {
			if(((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof ANumberExpression) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression)((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ANumberExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMultExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b = 0;
				String name1 = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				String name2 = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name1) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
					if( ((Variable) vartable.get(i)).getName().equals(name2) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((AMultExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((AMultExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((AMultExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((AMultExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AIdentifierExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((AMultExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((AMultExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((AMultExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((AMultExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				String name = ((AIdentifierExpression) ((AMultExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				int b = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AAddExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				int b = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				int b = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ASubExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMultExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				int b = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof ADivExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ADivExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof APlusplusExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (APlusplusExpression) ((AMultExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
			else if( ((AMultExpression) node).getL() instanceof AMinusminusExpression && ((AMultExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMinusminusExpression) ((AMultExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((AMultExpression) node).getL() );
				result = a * b;
			}
		}
		else if(node instanceof ADivExpression) {
			if(((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof ANumberExpression) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression)((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ANumberExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				int b = 0;
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getR()).getNum().toString().trim() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof ANumberExpression ) {
				int a = Integer.parseInt( ((ANumberExpression) ((ADivExpression) node).getL()).getNum().toString().trim() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b = 0;
				String name1 = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				String name2 = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name1) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
					if( ((Variable) vartable.get(i)).getName().equals(name2) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						b = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((ADivExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((ADivExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((ADivExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((ADivExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AIdentifierExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getL()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AAddExpression) ((ADivExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ASubExpression) ((ADivExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMultExpression) ((ADivExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (ADivExpression) ((ADivExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof AIdentifierExpression ) {
				int a = 0;
				int b =  getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				String name = ((AIdentifierExpression) ((ADivExpression) node).getR()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				int b = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AAddExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				int b = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof AAddExpression ) {
				int a = getResult( (AAddExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				int b = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ASubExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof ASubExpression ) {
				int a = getResult( (ASubExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMultExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof AMultExpression ) {
				int a = getResult( (AMultExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				int b = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof ADivExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (ADivExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof ADivExpression ) {
				int a = getResult( (ADivExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				int b = getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof APlusplusExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (APlusplusExpression) ((ADivExpression) node).getL() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof APlusplusExpression ) {
				int a = getResult( (APlusplusExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
			else if( ((ADivExpression) node).getL() instanceof AMinusminusExpression && ((ADivExpression) node).getR() instanceof AMinusminusExpression ) {
				int a = getResult( (AMinusminusExpression) ((ADivExpression) node).getR() );
				int b = getResult( (AMinusminusExpression) ((ADivExpression) node).getL() );
				result = a / b;
			}
		}
		else if(node instanceof APlusplusExpression) {
			if( ((APlusplusExpression) node).getExpression() instanceof ANumberExpression) {
				int a = Integer.parseInt( ((ANumberExpression) ((APlusplusExpression) node).getExpression()).getNum().toString().trim() );
				result = ++a;
			}
			else if( ((APlusplusExpression) node).getExpression() instanceof AIdentifierExpression) {
				int a = 0;
				String name = ((AIdentifierExpression) ((APlusplusExpression) node).getExpression()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				if(a!=0)  result = ++a;
				else result = a;
			}
			else if(((APlusplusExpression) node).getExpression() instanceof AAddExpression || ((APlusplusExpression) node).getExpression() instanceof ASubExpression 
					|| ((APlusplusExpression) node).getExpression() instanceof AMultExpression || ((APlusplusExpression) node).getExpression() instanceof ADivExpression
					|| ((APlusplusExpression) node).getExpression() instanceof APlusplusExpression || ((APlusplusExpression) node).getExpression() instanceof AMinusminusExpression) {
				
				int a = getResult(((APlusplusExpression) node).getExpression());
				result = ++a;			
			}
		}
		else if(node instanceof AMinusminusExpression) {
			if( ((AMinusminusExpression) node).getExpression() instanceof ANumberExpression) {
				int a = Integer.parseInt( ((ANumberExpression) ((AMinusminusExpression) node).getExpression()).getNum().toString().trim() );
				result = --a;
			}
			else if( ((AMinusminusExpression) node).getExpression() instanceof AIdentifierExpression) {
				int a = 0;
				String name = ((AIdentifierExpression) ((AMinusminusExpression) node).getExpression()).getId().toString().trim();
				for(int i = 1; i <= vartable.size(); i++) {
					if( ((Variable) vartable.get(i)).getName().equals(name) && ((Variable) vartable.get(i)).getType().equals("number") ) {
						a = Integer.parseInt( ((Variable) vartable.get(i)).getValue() );
					}
				}
				if(a!=0)  result = --a;
				else result = a;
			}
			else if(((AMinusminusExpression) node).getExpression() instanceof AAddExpression || ((AMinusminusExpression) node).getExpression() instanceof ASubExpression 
					|| ((AMinusminusExpression) node).getExpression() instanceof AMultExpression || ((AMinusminusExpression) node).getExpression() instanceof ADivExpression
					|| ((AMinusminusExpression) node).getExpression() instanceof APlusplusExpression || ((AMinusminusExpression) node).getExpression() instanceof AMinusminusExpression) {
				
				int a = getResult(((AMinusminusExpression) node).getExpression());
				result = --a;			
			}
		}
		return result;
	}
	
}
