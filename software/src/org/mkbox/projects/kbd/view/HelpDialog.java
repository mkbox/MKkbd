package org.mkbox.projects.kbd.view;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class HelpDialog {

	@FXML
	private WebView htmlArea;
	
	private Stage dialogStage;
	
	public HelpDialog() {
		
	}
	
	@FXML
	private void initialize() {
		
	}
	
	@FXML
	private void closeHandle() {
		dialogStage.close();
	}
	
	public void setDialogStage(Stage stage)	{
		dialogStage = stage;
	}
	
	public void loadHtml(String link) {
		htmlArea.getEngine().load(link);
	}

}
