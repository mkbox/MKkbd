package org.mkbox.projects.kbd.model;

import javax.xml.bind.annotation.XmlElement;

public class Key{
	
	private String keyName;
	private Integer keyNum;
	private Integer keyCode;
	
	public Key() {
		keyName = "";
		keyNum = -1;
		keyCode = -1;
	}
	
	public Key(int num, int code, String name) {
		this.keyNum = num;
		this.keyCode = code;
		this.keyName = name;
	}
	
	@XmlElement(name = "Name")
	public String getKeyName() {
		return keyName;
	}
	
	public void setKeyName(String name) {
		keyName = name;
	}
	
	@XmlElement(name = "Number")
	public int getKeyNum() {
		return keyNum;
	}
	
	public void setKeyNum(int num) {
		keyNum = num;
	}
	
	@XmlElement(name = "Code")
	public int getKeyCode() {
		return keyCode;
	}
	
	public void setKeyCode(int nCode) {
		keyCode = nCode;
	}
}
