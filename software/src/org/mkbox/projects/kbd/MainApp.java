package org.mkbox.projects.kbd;

import org.mkbox.projects.kbd.model.Key;
import org.mkbox.projects.kbd.model.KeyDataWrapper;
import org.mkbox.projects.kbd.model.KeyList;
import org.mkbox.projects.kbd.view.HelpDialog;
import org.mkbox.projects.kbd.view.InputDialog;
import org.mkbox.projects.kbd.view.MainView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.Timer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import purejavahidapi.*;

/*version: 0.3*/
public class MainApp extends Application {

	private Stage primaryStage;
	private BorderPane rootLayout;
	private HidDeviceInfo deviceInfo;
	private HidDevice device;
	private ArrayList<String> messages;
	private final short vid = 0x16C0;
	private final short pid = 0x06DC;
	private int keyCount = 12;
	private final int reportSize = 128;
	private Timer timer;
	private boolean timerStop = true;
	private ObservableList<KeyList> keyData = FXCollections.observableArrayList();
	private Map<KeyCode, Integer> keyCodes;
	private Map<Integer, String> keyNames;
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public MainApp() {
		for (int i = 0; i < keyCount; i++) {
			keyData.add(
					new KeyList(
						new Key(i+1, 0x29, "Esc"), 
						new Key(i+1, 0x29, "Esc"), 
						new Key(i+1, 0x29, "Esc"), 
						new Key(i+1, 0x29, "Esc")
					)
				);
		}
		keyNames = new Hashtable<Integer, String>(64);
		keyNames.put(0x04, "A");
		keyNames.put(0x05, "B");
		keyNames.put(0x06, "C");
		keyNames.put(0x07, "D");
		keyNames.put(0x08, "E");
		keyNames.put(0x09, "F");
		keyNames.put(0x0A, "G");
		keyNames.put(0x0B, "H");
		keyNames.put(0x0C, "I");
		keyNames.put(0x0D, "J");
		keyNames.put(0x0E, "K");
		keyNames.put(0x0F, "L");
		keyNames.put(0x10, "M");
		keyNames.put(0x11, "N");
		keyNames.put(0x12, "O");
		keyNames.put(0x13, "P");
		keyNames.put(0x14, "Q");
		keyNames.put(0x15, "R");
		keyNames.put(0x16, "S");
		keyNames.put(0x17, "T");
		keyNames.put(0x18, "U");
		keyNames.put(0x19, "V");
		keyNames.put(0x1A, "W");
		keyNames.put(0x1B, "X");
		keyNames.put(0x1C, "Y");
		keyNames.put(0x1D, "Z");
		keyNames.put(0x1E, "1");
		keyNames.put(0x1F, "2");
		keyNames.put(0x20, "3");
		keyNames.put(0x21, "4");
		keyNames.put(0x22, "5");
		keyNames.put(0x23, "6");
		keyNames.put(0x24, "7");
		keyNames.put(0x25, "8");
		keyNames.put(0x26, "9");
		keyNames.put(0x27, "0");
		keyNames.put(0x28, "Enter");
		keyNames.put(0x29, "Esc");
		keyNames.put(0x2A, "BkSpc");
		keyNames.put(0x2B, "Tab");
		keyNames.put(0x2C, "Space");
		keyNames.put(0x2D, "-");
		keyNames.put(0x2E, "=");
		keyNames.put(0x2F, "[");
		keyNames.put(0x30, "]");
		keyNames.put(0x31, "\\");
		keyNames.put(0x33, ";");
		keyNames.put(0x34, "'");
		keyNames.put(0x35, "`");
		keyNames.put(0x36, ",");
		keyNames.put(0x37, ".");
		keyNames.put(0x38, "/");
		keyNames.put(0x39, "CapsLock");
		keyNames.put(0x3A, "F1");
		keyNames.put(0x3B, "F2");
		keyNames.put(0x3C, "F3");
		keyNames.put(0x3D, "F4");
		keyNames.put(0x3E, "F5");
		keyNames.put(0x3F, "F6");
		keyNames.put(0x40, "F7");
		keyNames.put(0x41, "F8");
		keyNames.put(0x42, "F9");
		keyNames.put(0x43, "F10");
		keyNames.put(0x44, "F11");
		keyNames.put(0x45, "F12");
		keyNames.put(0x46, "PrtScn");
		keyNames.put(0x47, "ScrollLock");
		keyNames.put(0x48, "Break");
		keyNames.put(0x49, "Insert");
		keyNames.put(0x4A, "Home");
		keyNames.put(0x4B, "PageUp");
		keyNames.put(0x4C, "Delete");
		keyNames.put(0x4D, "End");
		keyNames.put(0x4E, "PageDn");
		keyNames.put(0x4F, "Right");
		keyNames.put(0x50, "Left");
		keyNames.put(0x51, "Down");
		keyNames.put(0x52, "Up");
		keyNames.put(0x53, "NumLock");
		keyCodes = new Hashtable<KeyCode, Integer>(64);
		keyCodes.put(KeyCode.A, 0x04);
		keyCodes.put(KeyCode.B, 0x05);
		keyCodes.put(KeyCode.C, 0x06);
		keyCodes.put(KeyCode.D, 0x07);
		keyCodes.put(KeyCode.E, 0x08);
		keyCodes.put(KeyCode.F, 0x09);
		keyCodes.put(KeyCode.G, 0x0A);
		keyCodes.put(KeyCode.H, 0x0B);
		keyCodes.put(KeyCode.I, 0x0C);
		keyCodes.put(KeyCode.J, 0x0D);
		keyCodes.put(KeyCode.K, 0x0E);
		keyCodes.put(KeyCode.L, 0x0F);
		keyCodes.put(KeyCode.M, 0x10);
		keyCodes.put(KeyCode.N, 0x11);
		keyCodes.put(KeyCode.O, 0x12);
		keyCodes.put(KeyCode.P, 0x13);
		keyCodes.put(KeyCode.Q, 0x14);
		keyCodes.put(KeyCode.R, 0x15);
		keyCodes.put(KeyCode.S, 0x16);
		keyCodes.put(KeyCode.T, 0x17);
		keyCodes.put(KeyCode.U, 0x18);
		keyCodes.put(KeyCode.V, 0x19);
		keyCodes.put(KeyCode.W, 0x1A);
		keyCodes.put(KeyCode.X, 0x1B);
		keyCodes.put(KeyCode.Y, 0x1C);
		keyCodes.put(KeyCode.Z, 0x1D);
		keyCodes.put(KeyCode.DIGIT1, 0x1E);
		keyCodes.put(KeyCode.DIGIT2, 0x1F);
		keyCodes.put(KeyCode.DIGIT3, 0x20);
		keyCodes.put(KeyCode.DIGIT4, 0x21);
		keyCodes.put(KeyCode.DIGIT5, 0x22);
		keyCodes.put(KeyCode.DIGIT6, 0x23);
		keyCodes.put(KeyCode.DIGIT7, 0x24);
		keyCodes.put(KeyCode.DIGIT8, 0x25);
		keyCodes.put(KeyCode.DIGIT9, 0x26);
		keyCodes.put(KeyCode.DIGIT0, 0x27);
		keyCodes.put(KeyCode.ENTER, 0x28);
		keyCodes.put(KeyCode.ESCAPE, 0x29);
		keyCodes.put(KeyCode.BACK_SPACE, 0x2A);
		keyCodes.put(KeyCode.TAB, 0x2B);
		keyCodes.put(KeyCode.SPACE, 0x2C);
		keyCodes.put(KeyCode.MINUS, 0x2D);
		keyCodes.put(KeyCode.EQUALS, 0x2E);
		keyCodes.put(KeyCode.OPEN_BRACKET, 0x2F);
		keyCodes.put(KeyCode.CLOSE_BRACKET, 0x30);
		keyCodes.put(KeyCode.BACK_SLASH, 0x31);
		keyCodes.put(KeyCode.SEMICOLON, 0x33);
		keyCodes.put(KeyCode.QUOTE, 0x34);
		keyCodes.put(KeyCode.BACK_QUOTE, 0x35);
		keyCodes.put(KeyCode.COMMA, 0x36);
		keyCodes.put(KeyCode.PERIOD, 0x37);
		keyCodes.put(KeyCode.SLASH, 0x38);
		keyCodes.put(KeyCode.CAPS, 0x39);
		keyCodes.put(KeyCode.F1, 0x3A);
		keyCodes.put(KeyCode.F2, 0x3B);
		keyCodes.put(KeyCode.F3, 0x3C);
		keyCodes.put(KeyCode.F4, 0x3D);
		keyCodes.put(KeyCode.F5, 0x3E);
		keyCodes.put(KeyCode.F6, 0x3F);
		keyCodes.put(KeyCode.F7, 0x40);
		keyCodes.put(KeyCode.F8, 0x41);
		keyCodes.put(KeyCode.F9, 0x42);
		keyCodes.put(KeyCode.F10, 0x43);
		keyCodes.put(KeyCode.F11, 0x44);
		keyCodes.put(KeyCode.F12, 0x45);
		keyCodes.put(KeyCode.PRINTSCREEN, 0x46);
		keyCodes.put(KeyCode.SCROLL_LOCK, 0x47);
		keyCodes.put(KeyCode.PAUSE, 0x48);
		keyCodes.put(KeyCode.INSERT, 0x49);
		keyCodes.put(KeyCode.HOME, 0x4A);
		keyCodes.put(KeyCode.PAGE_UP, 0x4B);
		keyCodes.put(KeyCode.DELETE, 0x4C);
		keyCodes.put(KeyCode.END, 0x4D);
		keyCodes.put(KeyCode.PAGE_DOWN, 0x4E);
		keyCodes.put(KeyCode.RIGHT, 0x4F);
		keyCodes.put(KeyCode.LEFT, 0x50);
		keyCodes.put(KeyCode.DOWN, 0x51);
		keyCodes.put(KeyCode.UP, 0x52);
		keyCodes.put(KeyCode.NUM_LOCK, 0x53);
		messages = new ArrayList<String>();
	}
	
	public ObservableList<KeyList> getKeyData() {
		return keyData;
	}
	
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("MK:Kbd charset changer");
		this.primaryStage.getIcons().add(new Image("file:resources/images/icon2.png"));
		initRootLayout();
		showLayouts();
	}

	private void showLayouts() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/KeySet.fxml"));
			AnchorPane keyset = (AnchorPane) loader.load();
			rootLayout.setCenter(keyset);
			MainView controller = new MainView();
			controller = loader.getController();
			controller.setMainApp(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/Menu.fxml"));
			rootLayout = (BorderPane) loader.load();
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.setMinWidth(640);
			primaryStage.setMinHeight(480);
			primaryStage.initStyle(StageStyle.UNIFIED);
			primaryStage.setOnCloseRequest((event) -> event.consume());
			primaryStage.resizableProperty().setValue(false);
			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public HidDeviceInfo findDevice() {
		List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
		HidDeviceInfo devInfo = null;
		for (HidDeviceInfo info : devList) {
			if (info.getVendorId() == vid && info.getProductId() == pid) {
				devInfo = info;
				break;
			}
		}
		return devInfo;
	}
	
	public HidDeviceInfo getDevice() {
		return this.deviceInfo;
	}
	
	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	public List<KeyList> getkeyData() {
		return keyData;
	}
	
	public File getKeyListFilePath() {
		Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
	    String filePath = prefs.get("filePath", null);
	    if (filePath != null) {
	        return new File(System.getProperty("user.dir"));
	    } else {
	        return null;
	    }
	}
	
	public boolean showHelpDialog() {
		try{
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/HelpDialog.fxml"));
			BorderPane page = (BorderPane) loader.load();
			
			Stage dialogStage = new Stage();
			dialogStage.setResizable(false);
			dialogStage.setTitle("Information");
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(primaryStage);
			dialogStage.initStyle(StageStyle.UTILITY);
			
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);
			
			HelpDialog controller = loader.getController();
			controller.setDialogStage(dialogStage);
			String readme = MainApp.class.getResource("README.HTML").toExternalForm();
			controller.loadHtml(readme);
			
			dialogStage.showAndWait();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean showInputDialog(int row, int col) {
		try{
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/InputDialog.fxml"));
			BorderPane page = (BorderPane) loader.load();
			
			Stage dialogStage = new Stage();
			dialogStage.setResizable(false);
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);
			InputDialog controller = loader.getController();
			scene.setOnKeyPressed(event -> {
				controller.setFound(false);
				if(keyCodes.containsKey(event.getCode())) {
					controller.setErr("");
					Key newKey = new Key(row+1, keyCodes.get(event.getCode()), keyNames.get(keyCodes.get(event.getCode())));
					keyData.get(row).setKey(col, newKey);
					keyData.get(row).generateDisplayName();
					controller.setFound(true);
					dialogStage.hide();
				} else {
					controller.setErr("This key is not supported.");
				}
				if(event.getCode() == KeyCode.ESCAPE && event.isControlDown()) {
					dialogStage.hide();
				}
			});
			dialogStage.initStyle(StageStyle.UNDECORATED);
			dialogStage.showAndWait();
			return controller.isKeyFound();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void setKeyListFilePath(File file) {
	    Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
	    if (file != null) {
	        prefs.put("filePath", file.getPath());
	        primaryStage.setTitle("AddressApp - " + file.getName());
	    } else {
	        prefs.remove("filePath");
	        primaryStage.setTitle("AddressApp");
	    }
	}
	
	public void loadKeysDataFromFile(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance(KeyDataWrapper.class);
			Unmarshaller um = context.createUnmarshaller();
			KeyDataWrapper wrapper = (KeyDataWrapper) um.unmarshal(file);
			keyData.clear();
			keyData.addAll(wrapper.getLayouts());
			for( KeyList item : keyData ) {
				item.generateDisplayName();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Unable to load data");
			alert.setContentText("Could not load data from file:\n" + file.getPath());
			alert.showAndWait();
		}
	}
	
	public void saveKeyDataToFile(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance(KeyDataWrapper.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			KeyDataWrapper wrapper = new KeyDataWrapper();
			wrapper.setLayouts(keyData);
			m.marshal(wrapper, file);
		} catch (Exception e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Unable to save data");
			alert.setContentText("Could not save data to file:\n" + file.getPath());
			alert.showAndWait();
		}
	}

	public void writeLayouts() {
		byte[] lay1 = new byte[keyCount];
		byte[] lay2 = new byte[keyCount];
		byte[] lay3 = new byte[keyCount];
		byte[] lay4 = new byte[keyCount];
		byte[] data = new byte[reportSize];
		for( int i = 0; i < keyCount; i++ ) {
			List<Key> keys = keyData.get(i).getKeys();
			lay1[i] = (byte) keys.get(0).getKeyCode();
			lay2[i] = (byte) keys.get(1).getKeyCode();
			lay3[i] = (byte) keys.get(2).getKeyCode();
			lay4[i] = (byte) keys.get(3).getKeyCode();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			out.write(1);
			out.write(lay1);
			out.write(lay2);
			out.write(lay3);
			out.write(lay4);
			
			byte[] fill = new byte[data.length-out.size()];
			for (int i = 0; i < fill.length; i++) {
				fill[i] = (byte) 0xFF;
			}
			
			out.write(fill);
		} catch (IOException e) {
			e.printStackTrace();
		}
		data = out.toByteArray();
		//messages.add(bytesToHex(data));
		if (device.setOutputReport((byte) 2, data, data.length) == reportSize) {
			messages.add("Layouts written to device.");
		} else {
			messages.add("Layouts not written to device.");
		}
	}
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public boolean connectDevice() {
		deviceInfo = findDevice();
		if (deviceInfo != null) {
			try {
				device = PureJavaHidApi.openDevice(deviceInfo);
				device.setDeviceRemovalListener(new DeviceRemovalListener() {
					@Override
					public void onDeviceRemoval(HidDevice source) {
						deviceInfo = null;
						device = null;
						messages.add("Device removed.");
					}
				});
				messages.add("Device connected.");
				int newCount = getKeyNum(getFeatureReport());
				if((newCount > 0) && (newCount != keyCount)){
					if(newCount > keyCount) {
						for(int i = keyCount; i < newCount; i++){
							keyData.add(
									new KeyList(
										new Key(i+1, 0x29, "Esc"), 
										new Key(i+1, 0x29, "Esc"), 
										new Key(i+1, 0x29, "Esc"), 
										new Key(i+1, 0x29, "Esc")
									)
								);
						}
					} else {
						for(int i = keyCount-1; i >= newCount; i--){
							keyData.remove(i);
						}
					}
					keyCount = newCount;
				}
				return true;
			} catch (IOException e) {
				device = null;
			}
			if (device == null ){
				messages.add("Unable to connect to device.");				
			}
		} else {
			messages.add("Device not found.");
			deviceInfo = null;
			device = null;
		}
		return false;
	}

	public byte[] getFeatureReport() {
		timer = new Timer(2000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				timer.stop();
				timerStop = true;
			}
		});
		timerStop = false;
		timer.start();
		boolean wait = true;
		while (wait) {
			byte[] data = new byte[reportSize + 1];
			if ((device.getFeatureReport(data, data.length) >= 0)) {
				//messages.add(bytesToHex(data));
				return data;
			}
			if (timerStop) {
				wait = false;
				messages.add("Unable to read from device.");
			}
		}
		return null;
	}
	
	public Integer getKeyNum(byte[] data) {
		if(data != null){
			data = Arrays.copyOfRange(data, 0, 1);
			messages.add("Controller uses "+data[0]+" keys.");
			return (int) data[0];
		} else {
			messages.add("Unable to load key count.");
			return 0;
		}
	}
	
	public void readLayouts(byte[] data) {
		if(data != null){
			data = Arrays.copyOfRange(data, 1, data.length-1);
			keyData.clear();
			int i0, i1, i2, i3;
			for (int i = 0; i < keyCount; i++) {
				i0 = data[i];
				i1 = data[i+keyCount];
				i2 = data[i+keyCount*2];
				i3 = data[i+keyCount*3];
				keyData.add(
						new KeyList(
							new Key(i+1, i0, keyNames.get(i0)), 
							new Key(i+1, i1, keyNames.get(i1)), 
							new Key(i+1, i2, keyNames.get(i2)), 
							new Key(i+1, i3, keyNames.get(i3))
						)
					);
			}
			messages.add("Layouts read successfully.");
		} else {
			messages.add("Unable to load layouts.");
		}
	}

	public ArrayList<String> getMessages() {
		return messages;
	}

	public void clearMessages() {
		messages.clear();
	}
	
	public boolean isDeviceConnected() {
		if (device != null && deviceInfo != null) {
			return true;
		}
		return false;
	}

	public void disconnectDevice() {
		device.close();
		device = null;
		deviceInfo = null;
		messages.add("Device disconnected.");
	}
}
