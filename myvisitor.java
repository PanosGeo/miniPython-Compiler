import minipython.analysis.*;
import minipython.node.*;
import java.util.*;

public class myvisitor extends DepthFirstAdapter 
{
	private Hashtable vartable;
	private Hashtable functable;
	private int error, func_id, func_args, var_id, array_index, progr_line, temp;
	private Functions func;
	private boolean func_accept;

	myvisitor(Hashtable vartable, Hashtable functable) 
	{
		this.vartable = vartable;
		this.functable = functable;
	}
	
	public int getError() {
		return error;
	}
	
	public String CheckVariableDeclaration (AIdentifierExpression node) {
		String name = node.getId().toString().trim();
		if(node.parent() instanceof AIdentifierLeftbrExpression) name = name.concat("[]");
		int line = node.getId().getLine();
		int pos = node.getId().getPos();
		String type = "";
		boolean check = false;
		for(int i = 1; i <= vartable.size(); i++) {                                  // Looks in the hashtable of variables to check
			if( ( (Variable) vartable.get(i)).getName().equals(name)) {				// if a variable is declared
				check = true;
				type = ((Variable) vartable.get(i)).getType();
			}
		}
		if(!check && !(node.parent() instanceof AIdentifierLeftbrExpression)) {
			System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " isn't declared.");
			++error;
		}
		return type;
	}
	
	@Override
	public void inAFunction(AFunction node) {
		temp = error;
		String func_name =  ((AIdentifierExpression) node.getExpression()).getId().toString().trim();
		int line = ((AIdentifierExpression) node.getExpression()).getId().getLine();
		int pos = ((AIdentifierExpression) node.getExpression()).getId().getPos();
		LinkedList list =  node.getArgument();
		func_args = 0;
		func = new Functions(func_name);
		for(int i = 0; i < list.size(); i++) {
			++func_args;
			++var_id;
			Variable var = new Variable(((AIdentifierExpression) ((AArgument)list.get(i)).getL()).getId().toString().trim(), "null", var_id, "null");
			vartable.put(var_id, var);
			func.addArg( ((AIdentifierExpression) ((AArgument)list.get(i)).getL()).getId().toString().trim() );
			if( ((AArgument) list.get(i)).getR().size() > 0 )  // Method getR of AArgument gives a list which tells whether an argument is assigned a value
				func_args -= 1;  // That list can have only 1 element, so if there is an assignment it's size is 1. If there is an assignment operation in an argument, we consider there is no argument
			for(int j = 0; j < ((AArgument) list.get(i)).getExtraArgs().size(); j++) {    // Same check if there are more than one arguments
				++func_args;
				++var_id;
				var = new Variable(( (AIdentifierExpression) ( (AExtraArgs) ((AArgument)list.get(i)).getExtraArgs().get(j)).getL()).getId().toString().trim(), "null", var_id, "null");
				vartable.put(var_id, var);
				func.addArg( ( (AIdentifierExpression) ( (AExtraArgs) ((AArgument)list.get(i)).getExtraArgs().get(j)).getL()).getId().toString().trim() );
				if( ( ( (AExtraArgs) ((AArgument) list.get(i)).getExtraArgs().get(j)).getR().size() ) > 0 ) func_args -= 1;
			}
		}
		String duplicate = func.Duplicate();  // checks if an argument is declared more than once for example test(a,b,a) 
		if(duplicate.isEmpty()) {
			func_accept = true;
			for(int i = 1; i <= functable.size(); i++) {
				if(( ( (Functions)functable.get(i)).getName().equals(func_name) && ((Functions) functable.get(i)).getArgs() == func_args)) func_accept = false;
			}                                                         // checks if a function with the same name & number of parameters exists
			if(func_accept == false) {								 // if it does, since all variables are global, then it's parameters must be
				for(int i = 0; i < func_args; i++) {				// deleted from the hashtable since the function will not be kept
					vartable.remove(var_id);
					--var_id;
				}
				System.out.println("[" + line + "," + pos + "]" + ": Function " + func_name + " is already defined, with same number of parameters.");
				++error;
			}
		}
		else {
			System.out.println("[" + line + "," + pos + "]" + ": Argument " + duplicate + " is already declared in function " + func_name);
			++error;
		}
		
	}
	 	  
	@Override
	public void outAFunction(AFunction node) {
		++progr_line;
		if(temp == error) {                          // if no errors were found during the check of the function
			++func_id;								// it wil be stored in the hashtable of functions
			func.setId(func_id);
			func.setArgs(func_args);
			func.setNode(node);
			if(!(node.getStatement() instanceof AReturnExprStatement)) func.setReturnType("void");
			functable.put(func_id, func);
		}
	}
	
	@Override
	public void inAAddExpression(AAddExpression node) {
		boolean check = true;
		if(node.getL() instanceof AStringExpression) {                                                    // if the operand before the + operator is a
			String name = ((AStringExpression) node.getL()).getString().toString().trim();				 // string the addition cannot be performed
			int line = ((AStringExpression) node.getL()).getString().getLine();
			int pos = ((AStringExpression) node.getL()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used in arithmetic operations.");
			++error;
			check = false;
		}
		if(node.getL() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getL());				// checks if the variable give as an operand before
			if(!type.isEmpty() && type.equals("string")) {												// + operator is a string.If it is the addition
				String name = ((AIdentifierExpression) node.getL()).getId().toString().trim();			// cannot be performed
				int line = ((AIdentifierExpression) node.getL()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Variable " + name + " is a string and can't be used in arithmetic operations.");
				++error;
				check = false;
			}
		}
		if(node.getL() instanceof ALeftbrIdentifierExpression) {
			int line = 0, pos = 0;                                                                                    // if the operand before the + operator
			if( ((ALeftbrIdentifierExpression) node.getL()).getL() instanceof ANumberExpression ) {					 // is a list it cannot be used for arithmetic
				line = ( (ANumberExpression) ((ALeftbrIdentifierExpression) node.getL()).getL()).getNum().getLine();  // operations. Lists can have numbers or strings
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
			name = name.concat("[]");                                                                                               // Arrays are stored as Variable
			String type = "";																										// objects with a hashtable which holds
			int key = 0;																											// the array's elements. Each element is 
			if( ((AIdentifierLeftbrExpression) node.getL()).getR() instanceof ANumberExpression) {									// in the position shown by the key in the array
				key =  Integer.parseInt( ((ANumberExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getNum().toString().trim() );     // Depending on the expression inside the []
			}																																		// different checks take place in order to get the key
			else if(((AIdentifierLeftbrExpression) node.getL()).getR() instanceof AIdentifierExpression) {							// Once the key is obtained, the hashtable gives the type of the element
				name = ((AIdentifierExpression) ((AIdentifierLeftbrExpression) node.getL()).getR()).getId().toString().trim();		// in that position. If it's not a number, it can't be used in arithmetic
				int index = 0;																										// operations
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
		if(check) {     // if everything is ok for the left operand, same checks take place for the right
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
	public void inASubExpression(ASubExpression node) {                                                 // Same checks as for addition
		boolean check = true;
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
	public void inAMultExpression(AMultExpression node) {
		boolean check = true;
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
	public void inADivExpression(ADivExpression node) {
		boolean check = true;
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
	public void inAPlusplusExpression(APlusplusExpression node) {
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
	public void inAMinusminusExpression(AMinusminusExpression node) {
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
	public void inAAssignmentStatement(AAssignmentStatement node) {
		temp = error;
	}
	
	@Override
	public void outAAssignmentStatement(AAssignmentStatement node) {
		++progr_line;
		if(temp == error) {
			String varname = ((AIdentifierExpression) node.getL()).getId().toString().trim();  // Method getL() of AAsignmentStatement gets the identifier before the = operator
			String vartype = "";
			String value = "";
			if(node.getR() instanceof AAddExpression || node.getR() instanceof ASubExpression || node.getR() instanceof AMultExpression || node.getR() instanceof ADivExpression
				|| node.getR() instanceof APlusplusExpression || node.getR() instanceof AMinusminusExpression) {
				vartype = "number";
				value = "" + getResult(node.getR()) + "";

			}
			else if(node.getR() instanceof ANumberExpression) {											// Depending on the expression on the right of the = operator
				vartype = "number";																		// different checks are made to obtain the value and the type
				value = ((ANumberExpression) node.getR()).getNum().toString().trim();					// assigned to a variable
			}																				
			else if(node.getR() instanceof AStringExpression) {
				vartype = "string";
				value = ((AStringExpression) node.getR()).getString().toString().trim();
			}
			else if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression)node.getR());              // Checks if the identifier on the rigth is declared
				if(!type.isEmpty()) {																	// If it is, gets its value in order to assign it
					vartype = type;
					String name = ((AIdentifierExpression)node.getR()).getId().toString().trim();
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable)vartable.get(i)).getName().equals(name) ) {
							value = ((Variable)vartable.get(i)).getValue();
						}
					}
				}
			}
			boolean enter = true;
			for(int i = 1; i <= vartable.size(); i++) {                  			   // Checks if the variable is already declared. If it is,
				if( ( (Variable)vartable.get(i)).getName().equals(varname)) {         // then changes the type of the variable to the one on the left of 
					( (Variable) vartable.get(i)).setType(vartype);					 // the = operator and informs that the variable must not be added to
					enter = false;										 			// the symbol table
					( (Variable) vartable.get(i)).setValue(value);
				}
			}
			if(enter) {
				++var_id;																// If the variable isn't already declared, it gets declared and
				Variable var = new Variable(varname, vartype, var_id, value);			// is added to the hashtable of variables
				vartable.put(var_id, var);
			}
		}
	}
	
	@Override
	public void inAMinusEqStatement(AMinusEqStatement node) {
		++progr_line;
		String varname = ((AIdentifierExpression) node.getL()).getId().toString().trim();                     // Checks if the operator on the right is
		int line = ((AIdentifierExpression) node.getL()).getId().getLine();									 // not a number and prints an error message
		int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
		String vartype = CheckVariableDeclaration((AIdentifierExpression) node.getL());
		if(!vartype.equals("string")) {
			if(node.getR() instanceof AStringExpression) {
				varname = ( (AStringExpression) node.getR()).getString().toString().trim();
				line = ( (AStringExpression) node.getR()).getString().getLine();
				pos = ( (AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": String " + varname + "can't be used in arithmetic operations.");
				++error;
			}
			else if(node.getR() instanceof AIdentifierExpression) {
				vartype = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(!vartype.isEmpty() && vartype.equals("string")) {
					varname = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					line = ((AIdentifierExpression) node.getR()).getId().getLine();
					pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Variable " + varname + " is a string and can't be used in arithmetic operations.");
					++error;
				}
			}
		}
		else {
			System.out.println("[" + line + "," + pos + "]" + ": Variable " + varname + " is a string and can't be arithmetically altered.");
			++error;
		}
	}
	
	@Override
	public void inADivEqStatement(ADivEqStatement node) {                                        // Same as MinusEq
		++progr_line;
		String varname = ((AIdentifierExpression) node.getL()).getId().toString().trim();
		int line = ((AIdentifierExpression) node.getL()).getId().getLine();
		int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
		String vartype = CheckVariableDeclaration((AIdentifierExpression) node.getL());
		if(!vartype.equals("string")) {
			if(node.getR() instanceof AStringExpression) {
				varname = ( (AStringExpression) node.getR()).getString().toString().trim();
				line = ( (AStringExpression) node.getR()).getString().getLine();
				pos = ( (AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": String " + varname + "can't be used in arithmetic operations.");
				++error;
			}
			else if(node.getR() instanceof AIdentifierExpression) {
				vartype = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(!vartype.isEmpty() && vartype.equals("string")) {
					varname = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					line = ((AIdentifierExpression) node.getR()).getId().getLine();
					pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Variable " + varname + " is a string and can't be used in arithmetic operations.");
					++error;
				}
			}
		}
		else {
			System.out.println("[" + line + "," + pos + "]" + ": Variable " + varname + " is a string and can't be arithmetically altered.");
			++error;
		}
	}
	
	@Override
	public void inAReturnExprStatement(AReturnExprStatement node) {
		++progr_line;
		if(!(node.parent() instanceof AFunction)) {
			System.out.println("Line " + progr_line + ": Return without a function statement.");
		}
		else {
			if(node.getExpression() instanceof AStringExpression) func.setReturnType("string");
			if(node.getExpression() instanceof AAddExpression || node.getExpression() instanceof ASubExpression || node.getExpression() instanceof AMultExpression || node.getExpression() instanceof ADivExpression
			   || node.getExpression() instanceof APlusplusExpression || node.getExpression() instanceof AMinusminusExpression || node.getExpression() instanceof ANumberExpression) {
					func.setReturnType("number");
			}
		}
	}
	
	@Override
	public void outAPrintExprStatement(APrintExprStatement node) {
		++progr_line;
	}
	
	@Override
	public void inAIdentifierLeftbrExpression(AIdentifierLeftbrExpression node) {
		String exists = CheckVariableDeclaration( (AIdentifierExpression) node.getL());                           // Checks if the array is declared
		if(!exists.isEmpty()) {																					 // If it is, checks the expression inside the []
			if(node.getR() instanceof AIdentifierExpression) {
				String type = CheckVariableDeclaration((AIdentifierExpression) node.getR());
				if(type.equals("string")) {
					String name = ((AIdentifierExpression) node.getR()).getId().toString().trim();
					int line = ((AIdentifierExpression) node.getR()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used to index arrays.");
					++error;
				}
			}
			else if(node.getR() instanceof AStringExpression) {
				String name = ((AStringExpression) node.getR()).getString().toString().trim();
				int line = ((AStringExpression) node.getR()).getString().getLine();
				int pos = ((AStringExpression) node.getR()).getString().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used to index arrays.");
				++error;
			}
			else if(node.getR() instanceof ALeftbrIdentifierExpression) {
				String name = ((AIdentifierExpression) node.getR()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getR()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getR()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used to index arrays.");
				++error;
			}
		}
		else {
			String name = ((AIdentifierExpression) node.getL()).getId().toString().trim();
			int line = ((AIdentifierExpression) node.getL()).getId().getLine();
			int pos = ((AIdentifierExpression) node.getL()).getId().getPos();
			System.out.println("[" + line + "," + pos + "]: " + "Array " + name + " isn't declared.");
		}
	}
	
	@Override
	public void inALeftBranchStatement(ALeftBranchStatement node) {                                         // Checks the expression inside the []
		++progr_line;																					   // If it has mistakes, the assignment of a value
		temp =  error;																					  // fails
		if(node.getExpr1() instanceof AIdentifierExpression) {
			String type = CheckVariableDeclaration((AIdentifierExpression) node.getExpr1());
			if(type.equals("string")) {
				String name = ((AIdentifierExpression) node.getExpr1()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getExpr1()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getExpr1()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used to index arrays.");
				++error;
			}
		}
		else if(node.getExpr1() instanceof AStringExpression) {
			String name = ((AStringExpression) node.getExpr1()).getString().toString().trim();
			int line = ((AStringExpression) node.getExpr1()).getString().getLine();
			int pos = ((AStringExpression) node.getExpr1()).getString().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Token " + name + " is a string and can't be used to index arrays.");
			++error;
		}
		else if(node.getExpr1() instanceof ALeftbrIdentifierExpression) {
			String name = ((AIdentifierExpression) node.getExpression()).getId().toString().trim();
			int line = ((AIdentifierExpression) node.getExpression()).getId().getLine();
			int pos = ((AIdentifierExpression) node.getExpression()).getId().getPos();
			System.out.println("[" + line + "," + pos + "]" + ": Lists can't be used to index arrays.");
			++error;
		}
	}
	
	@Override
	public void outALeftBranchStatement(ALeftBranchStatement node) {                                              // If everything is ok, checks the array
		if(temp == error) {																						 // Because arrays are stored as Variable objects
			String arrayname = ((AIdentifierExpression) node.getExpression()).getId().toString().trim() + "[]";  // like variables, we add [] at the end to differentiate them
			String type = "";
			boolean enter = true;
			int index = 0, key = 0;
			for(int i = 1; i <= vartable.size(); i++) {                                              // Checks if the array is declared
				if( ( (Variable)vartable.get(i)).getName().equals(arrayname)) {
					enter = false;
					index = i;
				}
			}
			if(!enter) {                                                                                            // If it is, it gets the position inside the []
																												   // and then the type of the element to be assigned there
				if(node.getExpr1() instanceof ANumberExpression) {												  // and updates the array
					key =  Integer.parseInt( ((ANumberExpression) node.getExpr1()).getNum().toString().trim() );
				}
				else if(node.getExpr1() instanceof AIdentifierExpression) {
					String name = ((AIdentifierExpression) node.getExpr1()).getId().toString().trim();
					int index2 = 0;
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable) vartable.get(i)).getName().equals(name) ) index2 = i;
					}
					key = Integer.parseInt( ((Variable) vartable.get(index2)).getValue() );
				}
				else if(node.getExpr1() instanceof AAddExpression || node.getExpr1() instanceof ASubExpression || node.getExpr1() instanceof AMultExpression
						|| node.getExpr1() instanceof ADivExpression || node.getExpr1() instanceof APlusplusExpression || node.getExpr1() instanceof AMinusminusExpression) {
					key = getResult(node.getExpr1());
				}
				if(node.getExpr2() instanceof AAddExpression || node.getExpr2() instanceof ASubExpression || node.getExpr2() instanceof AMultExpression || node.getExpr2() instanceof ADivExpression
			       || node.getExpr2() instanceof APlusplusExpression || node.getExpr2() instanceof AMinusminusExpression || node.getExpr2() instanceof ANumberExpression) {
						type = "number";
				}
				else if(node.getExpr2() instanceof AStringExpression) type = "string";
				else if(node.getExpr2() instanceof AIdentifierExpression) type = CheckVariableDeclaration((AIdentifierExpression)node.getExpr2());
				( (Variable) vartable.get(index)).addElement(key, type);
			}
			else {                                                                                                      // if it isn't declared, does the same checks
				if(node.getExpr1() instanceof ANumberExpression) {														// and creates the array
					key =  Integer.parseInt( ((ANumberExpression) node.getExpr1()).getNum().toString().trim() );
				}
				else if(node.getExpr1() instanceof AIdentifierExpression) {
					String name = ((AIdentifierExpression) node.getExpr1()).getId().toString().trim();
					for(int i = 1; i <= vartable.size(); i++) {
						if( ((Variable) vartable.get(i)).getName().equals(name) ) index = i;
					}
					key = Integer.parseInt( ((Variable) vartable.get(index)).getValue() );
				}
				else if(node.getExpr1() instanceof AAddExpression || node.getExpr1() instanceof ASubExpression || node.getExpr1() instanceof AMultExpression
						|| node.getExpr1() instanceof ADivExpression || node.getExpr1() instanceof APlusplusExpression || node.getExpr1() instanceof AMinusminusExpression) {
					key = getResult(node.getExpr1());
				}
				if(node.getExpr2() instanceof AAddExpression || node.getExpr2() instanceof ASubExpression || node.getExpr2() instanceof AMultExpression || node.getExpr2() instanceof ADivExpression
			       || node.getExpr2() instanceof APlusplusExpression || node.getExpr2() instanceof AMinusminusExpression || node.getExpr2() instanceof ANumberExpression) {
						type = "number";
				}
				else if(node.getExpr2() instanceof AStringExpression) type = "string";
				else if(node.getExpr2() instanceof AIdentifierExpression) type = CheckVariableDeclaration((AIdentifierExpression)node.getExpr2());
				++var_id;
				Variable var = new Variable(arrayname, "array", var_id, "null");
				var.addElement(key, type);
				vartable.put(var_id, var);
			}
		} 
	}	
	
	@Override
	public void inAForStateStatement(AForStateStatement node) {
		++progr_line;
		String type = CheckVariableDeclaration((AIdentifierExpression) node.getIdentifierFor());
		String type2 =  CheckVariableDeclaration((AIdentifierExpression) node.getIdentifierIn());
		if(!type.isEmpty() && !type2.isEmpty()) {
			if(type.equals("string")) {
				String name = ((AIdentifierExpression) node.getIdentifierFor()).getId().toString().trim();
				int line = ((AIdentifierExpression) node.getIdentifierFor()).getId().getLine();
				int pos = ((AIdentifierExpression) node.getIdentifierFor()).getId().getPos();
				System.out.println("[" + line + "," + pos + "]: " + "Variable " + name + " is a string can't be used for range" );
				++error;
			}
			else {
				if(type2.equals("string")) {
					String name = ((AIdentifierExpression) node.getIdentifierIn()).getId().toString().trim();
					int line = ((AIdentifierExpression) node.getIdentifierIn()).getId().getLine();
					int pos = ((AIdentifierExpression) node.getIdentifierIn()).getId().getPos();
					System.out.println("[" + line + "," + pos + "]: " + "Variable " + name + " is a string can't be used for range" );
					++error;
				}
			}
		}
	}
	
	
	// Returns the result of arithmetic operations
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
