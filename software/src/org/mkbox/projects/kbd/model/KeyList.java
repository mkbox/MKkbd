package org.mkbox.projects.kbd.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class KeyList {

	private ArrayList<StringProperty> displayName;
	private ArrayList<Key> keys;
	
	public KeyList() {
		this.keys = new ArrayList<Key>(4);
		this.displayName = new ArrayList<StringProperty>(4); 
	}
	
	public KeyList(ArrayList<Key> keys) {
		this.keys = new ArrayList<Key>(4);
		this.displayName = new ArrayList<StringProperty>(4); 
		this.keys.addAll(keys);
		generateDisplayName();
	}
	
	public KeyList(Key key1, Key key2, Key key3, Key key4) {
		this.keys = new ArrayList<Key>(4);
		this.displayName = new ArrayList<StringProperty>(4); 
		this.keys.add(key1);
		this.keys.add(key2);
		this.keys.add(key3);
		this.keys.add(key4);
		generateDisplayName();
	}
	
	public void setKey(int num, Key key) {
		keys.remove(num);
		keys.add(num, key);
	}
	
	public StringProperty getNameCol1() {
		return displayName.get(0);
	}

	public StringProperty getNameCol2() {
		return displayName.get(1);
	}
	
	public StringProperty getNameCol3() {
		return displayName.get(2);
	}
	
	public StringProperty getNameCol4() {
		return displayName.get(3);
	}
	
	@XmlElement(name = "key")
	public List<Key> getKeys() {
		return keys;
	}
	public void generateDisplayName() {
		displayName.clear();
		for( int i = 0; i < 4; i++ ) {
			displayName.add(new SimpleStringProperty(keys.get(i).getKeyNum() + ":	" + keys.get(i).getKeyName()));
		}
	}
	
}
