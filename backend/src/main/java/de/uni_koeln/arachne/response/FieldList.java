package de.uni_koeln.arachne.response;

import java.util.ArrayList;
import java.util.List;

public class FieldList extends Content {
	public FieldList() {
		value = new ArrayList<String>();
	}
	
	private List<String> value = null;
	
	public List<String> getValue() {
		return this.value;
	}
	
	public void add(String value) {
		this.value.add(value);
	}
	
	public String get(int index) {
		return this.value.get(index);
	}
	
	public void modify(int index, String value) {
		this.value.set(index, value);
	}
	
	public boolean isEmpty() {
		return this.value.isEmpty();
	}
	
	public int size() {
		return this.value.size();
	}
}
