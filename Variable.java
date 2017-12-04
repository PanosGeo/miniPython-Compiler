import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class Variable {
	
	private String name;
	private String type;
	private String value;
	private int id;
	private Hashtable array;
	
	public Variable(String name, String type, int id, String value) {
		this.name = name;
		this.type = type;
		this.id = id;
		this.value = value;
		array = new Hashtable();
	}
	
	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	public String getValue() {
		return value;
	}
	
	public int getId() {
		return id;
	}
	
	public String getElement(int key) {
		if(!array.isEmpty()) return array.get(key).toString();
		else return "";
	}
	
	public int getSize() {
		return array.size();
	}
	
	public void addElement(int key, String type) {
		array.put(key, type);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void setId(int id) {
		this.id = id;
	}
}