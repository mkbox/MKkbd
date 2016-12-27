package org.mkbox.projects.kbd.view;

import java.io.File;

import org.mkbox.projects.kbd.MainApp;
import org.mkbox.projects.kbd.model.KeyList;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class MainView {

	@FXML
	private TableView<KeyList> keyTable;
	
	@FXML
	private TableColumn<KeyList, String> layCol1;
	
	@FXML
	private TableColumn<KeyList, String> layCol2;
	
	@FXML
	private TableColumn<KeyList, String> layCol3;
	
	@FXML
	private TableColumn<KeyList, String> layCol4;
	
	@FXML
	private TextArea logArea;
	
	@FXML
	private ToggleButton editSwitch;
	
	@FXML
	private Button readButton;
	
	@FXML
	private Button writeButton;
	
	@FXML
	private Button findButton;
	
	private ObservableList<TablePosition> selectedCells;
	private MainApp mainApp;
	private boolean editable = false;
	private int rowSelected;
	private int colSelected;

	private Timeline timeline;

	
	public MainView() {
		timeline = new Timeline (
		    new KeyFrame (
		        Duration.millis(1000), //1000 мс * 60 сек = 1 мин
		        ae -> {
					if(!mainApp.isDeviceConnected()) {
			            timeline.stop();
			            disableButtonsOnDisconnect();
						printMessages();
					}
		        }
		    )
		);
	}
	
	@FXML
	private void initialize() {
		layCol1.setCellValueFactory(cellData -> cellData.getValue().getNameCol1());
		layCol2.setCellValueFactory(cellData -> cellData.getValue().getNameCol2());
		layCol3.setCellValueFactory(cellData -> cellData.getValue().getNameCol3());
		layCol4.setCellValueFactory(cellData -> cellData.getValue().getNameCol4());
		
	}
	
	@FXML
	private void openHandle() {
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
		fileChooser.getExtensionFilters().add(extFilter);
		File oldFile = new File(mainApp.getKeyListFilePath().getParent());
		if( oldFile.isDirectory() ) {
			fileChooser.setInitialDirectory((new File(mainApp.getKeyListFilePath().getParent())));
		}
		File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());
		if( file != null ) {
			mainApp.setKeyListFilePath(file);
			mainApp.loadKeysDataFromFile(file);
			logAddLine("Config loaded");
		}
	}
	
	@FXML
	private void saveHandle() {
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
		fileChooser.getExtensionFilters().add(extFilter);
		File oldFile = new File(mainApp.getKeyListFilePath().getParent());
		if( oldFile.isDirectory() ) {
			fileChooser.setInitialDirectory((new File(mainApp.getKeyListFilePath().getParent())));
		}
		File file = fileChooser.showSaveDialog(mainApp.getPrimaryStage());
		if( file != null ) {
			if(!file.getPath().endsWith(".xml")) {
				file = new File(file.getPath() + ".xml");
			}
			mainApp.setKeyListFilePath(file);
			mainApp.saveKeyDataToFile(file);
			logAddLine("Config saved");
		}
	}
	
	@FXML
	private void exitHandle() {
		System.exit(0);
	}
	
	@FXML
	private void editHandle() {
		editable = !editable;
		if(editable) {
			editSwitch.setSelected(true);
			keyTable.setDisable(false);
		} else {
			editSwitch.setSelected(false);
			keyTable.setDisable(true);
		}
	}
	
	@FXML
	private void writeHandle() {
		if(mainApp.isDeviceConnected()) {
			readButton.setDisable(true);
			writeButton.setDisable(true);
			mainApp.writeLayouts();
			readButton.setDisable(false);
			writeButton.setDisable(false);
		} else {
			readButton.setDisable(true);
			writeButton.setDisable(true);
		}
		printMessages();
	}
	
	@FXML
	private void readHandle() {
		if(mainApp.isDeviceConnected()) {
			readButton.setDisable(true);
			writeButton.setDisable(true);
			mainApp.readLayouts(mainApp.getFeatureReport());
			readButton.setDisable(false);
			writeButton.setDisable(false);
		} else {
			readButton.setDisable(true);
			writeButton.setDisable(true);
		}
		printMessages();
	}
	
	@FXML
	private void connectHandle() {
		if (!mainApp.isDeviceConnected()){
			if (mainApp.connectDevice()) {
				timeline.setCycleCount(Timeline.INDEFINITE);
				timeline.play();
				enableButtonsOnDisconnect();
			} else {
				timeline.stop();
				disableButtonsOnDisconnect();
			}
		}else{
			mainApp.disconnectDevice();
			timeline.stop();
			disableButtonsOnDisconnect();
		}
		printMessages();
	}
	
	@FXML
	private void helpHandle(){
		mainApp.showHelpDialog();
	}
	
	private void disableButtonsOnDisconnect() {
		readButton.setDisable(true);
		writeButton.setDisable(true);
		findButton.setText("Connect");
	}
	
	private void enableButtonsOnDisconnect() {
		readButton.setDisable(false);
		writeButton.setDisable(false);
		findButton.setText("Disconnect");
	}

	private void printMessages() {
		for(String s : mainApp.getMessages() ){
			logAddLine(s);
		}
		mainApp.clearMessages();
	}
	
	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
		keyTable.setItems(mainApp.getKeyData());;
		keyTable.getSelectionModel().setCellSelectionEnabled(true);
		selectedCells = keyTable.getSelectionModel().getSelectedCells();
		selectedCells.addListener(new ListChangeListener<TablePosition>() {
		    @Override
		    public void onChanged(Change change) {
		        for (TablePosition pos : selectedCells) {
		            colSelected = pos.getColumn();
		        }
		    };
		});
		keyTable.setRowFactory( tv -> {
		    TableRow<KeyList> row = new TableRow<>();
		    row.setOnMouseClicked(event -> {
		        if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
		            KeyList rowData = row.getItem();
		            logAddLine("Changing key #"+(row.getIndex()+1)+" for layout "+(colSelected+1));
		            rowSelected = row.getIndex();
		            if(mainApp.showInputDialog(rowSelected, colSelected)) {
		            	keyTable.refresh();
		            }
		        }
		    });
		    return row ;
		});
	}

	public int getRowSelected() {
		return rowSelected;
	}
	
	public int getColSelected() {
		return colSelected;
	}
	
	public void logAddLine(String s) {
		this.logArea.insertText(0, "\n");
		this.logArea.insertText(0, s);
	}
	
	public void logAdd(String s) {
		this.logArea.insertText(0, " "+s);
	}
	
	public void logClear() {
		this.logArea.clear();
	}
	
}

