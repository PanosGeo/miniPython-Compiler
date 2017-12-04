import java.util.ArrayList;
import minipython.node.AFunction;

public class Functions {

	private String name;
	private String return_type;
	private int args, id;
	private ArrayList<String> arg_names;
	private AFunction func;
	
	public Functions(String name) {
		this.name = name;
		arg_names = new ArrayList<String>();
	}
	
	public String getName() {
		return name;
	}
	
	public int getArgs() {
		return args;
	}
	
	public String getReturnType() {
		return return_type;
	}
	
	public int getId() {
		return id;
	}
	
	public AFunction getNode() {
		return func;
	}
	
	public String getArgName(int index) {
		if(!arg_names.isEmpty()) return arg_names.get(index);
		else return "No arguments";
	}
	
	public void addArg(String arg) {
		arg_names.add(arg);
	}
	
	public String Duplicate() {
		String duplicate = "";
		for(int i = 0; i < arg_names.size(); i++) {
			for(int j = 0; j < arg_names.size(); j++) {
				if(i == j) continue;
				if(arg_names.get(i).equals(arg_names.get(j))) duplicate = arg_names.get(i);
			}
		}
		return duplicate;
	}
	
	public String findArg(String arg) {
		String name = "";
		for(int i = 0; i < arg_names.size(); i++) {
			if(arg_names.get(i).equals(arg)) name = arg_names.get(i);
		}
		return name;
	}
	
	public int getSize() {
		return arg_names.size();
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setArgs(int args) {
		this.args = args;
	}
	
	public void setReturnType(String type) {
		return_type = type;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setNode(AFunction node) {
		func = node;
	}
	
}
