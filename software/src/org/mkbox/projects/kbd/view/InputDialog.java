package org.mkbox.projects.kbd.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;

public class InputDialog {

	private boolean found = false;
	@FXML
	private Label errLabel;
	
	@FXML
	private void initialize() {
		
	}
	
	@FXML
	private void keyPressedHandle(KeyEvent key) {
		System.out.println("Pressed: " + key.getCode());
	}
	
	public boolean isKeyFound() {
		return found;
	}
	
	public void setFound(boolean flag){
		found = flag;
	}
	
	public void setErr(String s) {
		errLabel.setText(s);
	}
	
}
