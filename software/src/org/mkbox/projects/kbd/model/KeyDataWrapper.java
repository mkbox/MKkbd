package org.mkbox.projects.kbd.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Layouts")
public class KeyDataWrapper {

	private List<KeyList> layouts;

	@XmlElement(name = "KeyList")
	public List<KeyList> getLayouts() {
		return layouts;
	}
	
	public void setLayouts(List<KeyList> newLayouts) {
		layouts = newLayouts;
	}
}
