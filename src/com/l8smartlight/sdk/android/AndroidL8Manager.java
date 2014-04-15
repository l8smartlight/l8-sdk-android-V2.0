package com.l8smartlight.sdk.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;

import com.l8smartlight.sdk.android.bluetooth.AndroidBluetoothL8;
import com.l8smartlight.sdk.android.bluetooth.BluetoothClient;
import com.l8smartlight.sdk.android.bluetooth.BtL8RegisteredStorer;
import com.l8smartlight.sdk.android.bluetooth.DeviceListActivity;
import com.l8smartlight.sdk.android.bluetooth.Preferences;
import com.l8smartlight.sdk.android.rest.AndroidRESTfulL8;
import com.l8smartlight.sdk.core.Color;
import com.l8smartlight.sdk.core.L8;
import com.l8smartlight.sdk.core.L8Exception;
import com.l8smartlight.sdk.core.Sensor;
import com.l8smartlight.sdk.core.L8.Animation;
import com.l8smartlight.sdk.core.L8.OnBooleanResultListener;
import com.l8smartlight.sdk.core.L8.OnColorMatrixResultListener;
import com.l8smartlight.sdk.core.L8.OnColorResultListener;
import com.l8smartlight.sdk.core.L8.OnEventListener;
import com.l8smartlight.sdk.core.L8.OnFloatResultListener;
import com.l8smartlight.sdk.core.L8.OnIntegerResultListener;
import com.l8smartlight.sdk.core.L8.OnSensorStatusListResultListener;
import com.l8smartlight.sdk.core.L8.OnSensorStatusResultListener;
import com.l8smartlight.sdk.core.L8.OnStringResultListener;
import com.l8smartlight.sdk.core.L8.OnVersionResultListener;
import com.l8smartlight.sdk.core.L8.Version;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AndroidL8Manager {

	private static final int REQUEST_ENABLE_BLUETOOTH = 3;
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;

	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	private BluetoothAdapter bluetoothAdapter;
	private ArrayList<AndroidL8ManagerListener> listeners = null;
	private ArrayList<String> registeredDevices = null;
	private ArrayList<AndroidBluetoothL8> connectedDevices = null;
	private HashMap<String, BluetoothClient> requestedConntections = null;

	private Context context = null;

	private boolean destroyed = false;

	private Preferences preferences;

	private boolean simulatorLoaded = false;
	private L8 simulator = null;

	private Handler bluetoothHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			String mac = msg.getData().getString(BluetoothClient.CURRENT_MAC);
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothClient.STATE_NONE:

					break;
				case BluetoothClient.STATE_CONNECTING:

					break;
				case BluetoothClient.STATE_CONNECTED: {
					if (!isRegisteredDevice(mac)) {
						registeredDevices.add(mac);
						BtL8RegisteredStorer.saveRegisteredL8(context, registeredDevices);
					}
					BluetoothClient bluetoothClient = requestedConntections.get(mac);
					requestedConntections.remove(mac);
					AndroidBluetoothL8 bluetoothL8 = new AndroidBluetoothL8(bluetoothClient);
					addOrReplaceConnectedL8(bluetoothL8);
					for (AndroidL8ManagerListener l : listeners) {
						l.deviceConnected(bluetoothL8);
					}
					if (requestedConntections.size() == 0) {
						for (AndroidL8ManagerListener l : listeners) {
							l.allConnectionsDone();
						}
					}
					break;
				}
				case BluetoothClient.STATE_FAILED: {
					BluetoothClient bluetoothClient = requestedConntections.get(mac);
					requestedConntections.remove(mac);
					for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
						try {
							if (connectedL8.getMac().compareTo(mac) == 0) {
								connectedDevices.remove(connectedL8);
								if (!isDestroyed()) {
									for (AndroidL8ManagerListener l : listeners) {
										l.deviceDisconnected(connectedL8);
									}
								}
							}
						} catch (Exception e) {
							Log.e("AndroidL8Manager", "Error " + e);
						}
					}
					if (requestedConntections.size() == 0 && connectedDevices.size() == 0) {
						if (!isDestroyed()) {
							for (AndroidL8ManagerListener l : listeners) {
								l.noDevicesConnected();
							}
						}
					} else if (requestedConntections.size() == 0 && connectedDevices.size() > 0 && bluetoothClient != null) {
						if (!isDestroyed()) {
							for (AndroidL8ManagerListener l : listeners) {
								l.allConnectionsDone();
							}
						}
					}
					break;
				}
				}
				break;
			case MESSAGE_READ:
				for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
					try {
						if (connectedL8.getMac().compareTo(mac) == 0) {
							connectedL8.received(msg.arg1, (byte[]) msg.obj);
							break;
						}
					} catch (Exception e) {
						Log.e("AndroidL8Manager", "Error " + e);
					}
				}
				break;
			case MESSAGE_DEVICE_NAME:

				break;
			case MESSAGE_TOAST:
				// TODO:
				/*
				 * if(m_bClosing==false) Toast.makeText(getApplicationContext(),
				 * msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				 */
				break;
			}
			return true;
		}
	});

	/**
	 * Creates an instance of AndroidL8Manager
	 * @param context    Context
	 */
	public AndroidL8Manager(Context context) {
		preferences = new Preferences(context);
		listeners = new ArrayList<AndroidL8ManagerListener>();
		connectedDevices = new ArrayList<AndroidBluetoothL8>();
		requestedConntections = new HashMap<String, BluetoothClient>();
		registeredDevices = BtL8RegisteredStorer.retriveRegisteredL8(context);
		if (registeredDevices == null) {
			registeredDevices = new ArrayList<String>();
		}
		this.context = context;
	}

	/**
	 * Registers listeners 
	 * @param listener  	Listener that will receive events like L8 connection status and so on.
	 */
	public void registerListener(AndroidL8ManagerListener listener) {
		listeners.add(listener);
	}

	/**
	 * Unregisters listeners 
	 * @param listener  	Listener to be removed from manager. You have to unregister a listener if it is not available any more.
	 */
	public void unregisterListener(AndroidL8ManagerListener listener) {
		for (AndroidL8ManagerListener l : listeners) {
			if (l.equals(listener)) {
				listeners.remove(listener);
				break;
			}
		}
	}
	
   /**
    * Initializes the current instance of AndroidL8Manager
    * @param activity      Activity is necessary to launch device list activity. 
    */
	public void init(Activity activity) {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			for (AndroidL8ManagerListener l : listeners) {
				l.bluetoothNotAvailable();
			}
			return;
		}
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
		} else {
			searchAndConnectWithRegisteredDevices();
		}
	}

	 /**
	  * Initializes the current instance of AndroidL8Manager. In the case of using l8 SDK from a service you have to call this method instead of <code>init(activity)</code>.  
	  */
	public void init() {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			for (AndroidL8ManagerListener l : listeners) {
				l.bluetoothNotAvailable();
			}
			return;
		}
		if (bluetoothAdapter.isEnabled()) {
			searchAndConnectWithRegisteredDevices();
		} else {
			for (AndroidL8ManagerListener l : listeners) {
				l.bluetoothNotEnabled();
			}
		}
	}

	private void searchAndConnectWithRegisteredDevices() {
		if (registeredDevices.size() == 0) {
			for (AndroidL8ManagerListener l : listeners) {
				l.noDevicesRegistered();
			}
		} else {
			for (String address : registeredDevices) {
				if (!isConnectedDevice(address)) {
					connect(address);
				}
			}
		}
	}

	private void connect(String address) {
		BluetoothClient client = new BluetoothClient(context, bluetoothHandler);
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
		requestedConntections.put(address, client);
		client.connect(device, true);
	}

	 /**
	  * Starts a new scan.
	  * @param activity      Activity is necessary to launch device list activity. 
	  */
	public void scan(Activity activity) {
		Intent intent = new Intent(context, DeviceListActivity.class);
		activity.startActivityForResult(intent, REQUEST_CONNECT_DEVICE_SECURE);
	}

	/**
	 * You have to call this method in your own onActivityResult. 
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				if (!this.isConnectedDevice(address)) {
					connect(address);
				}
				// connectDevice(data, true);
			} else {
				if (this.connectedDevices.size() == 0) {
					for (AndroidL8ManagerListener l : listeners) {
						l.noDevicesConnected();
					}
				}
			}
			break;
		case REQUEST_ENABLE_BLUETOOTH:
			if (resultCode == Activity.RESULT_OK) {
				searchAndConnectWithRegisteredDevices();
			} else {
				for (AndroidL8ManagerListener l : listeners) {
					l.bluetoothNotEnabled();
				}
			}
			break;
		}
	}

	private boolean isRegisteredDevice(String address) {
		for (String device : registeredDevices) {
			if (device.compareTo(address) == 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns <code>true</code> if a device is connected.
	 * @param address   Bluetooth address    
	 * @return          <code>true</code> if a device is connected.
	 */
	public boolean isConnectedDevice(String address) {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			try {
				if (connectedL8.getMac().compareTo(address) == 0) {
					return true;
				}
			} catch (Exception e) {
				Log.e("AndroidL8Manager", "Error " + e);
			}
		}
		return false;
	}

	private void addOrReplaceConnectedL8(AndroidBluetoothL8 bluetoothL8) {
		try {
			for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
				if (connectedL8.getMac().compareTo(bluetoothL8.getMac()) == 0) {
					connectedDevices.remove(connectedL8);
					connectedDevices.add(bluetoothL8);
					return;
				}
			}
			connectedDevices.add(bluetoothL8);
		} catch (Exception e) {
			Log.e("AndroidL8Manager", "Error " + e);
		}
	}

	/**
	 * Returns a l8 devices list with all connected devices.
	 * @return l8 device list    
	 */
	public ArrayList<L8> getConnectedDeviceList() {
		ArrayList<L8> result = new ArrayList<L8>();
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			result.add(connectedL8);
		}
		return result;
	}

	/**
	 * Removes all registered devices. It is a very useful method to clean device list and reduce the initial connection time.
	 */
	public void removeRegisteredDevices(){
		registeredDevices = new ArrayList<String>();
		BtL8RegisteredStorer.saveRegisteredL8(context,registeredDevices);
	}
	
	/**
	 * Releases resources and close all connections. You have to call this method when you close your app. 
	 */
	public void onDestroy() {
		if (!isDestroyed()) {
			listeners.clear();
			for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
				connectedL8.closeConnection();
			}
			destroyed = true;
		}
	}

	/**
	 * Checks if AndroidL8manager has been destroyed.
	 * @return <code>true</code> if AndroidL8manager has been destroyed
	 */
	public boolean isDestroyed() {
		return destroyed;
	}

	/***********************************************************************
	 * Operations on L8s
	 ***********************************************************************/
	/**
	 * Sets a l8 image with text format.
	 * @param image         Image in text format. for example: 
	 * 						this is the Bluetooth symbol 						
	 *  					"#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#0000ff-#0000ff-#ffffff-#ffffff-#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#0000ff-#ffffff-#0000ff-#ffffff-#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#ffffff-#ffffff-#0000ff-#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#ffffff-#ffffff-#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#0000ff-#ffffff-#0000ff-#ffffff-#0000ff-#0000ff-#ffffff-#0000ff-#0000ff-#ffffff-#ffffff-#0000ff-#0000ff-#0000ff-#0000ff-#0000ff-#0000ff-#ffffff-#0000ff-#0000ff-#0000ff-#000000"
	 * @throws L8Exception  L8 generic exception.
	 */
	public void setMatrix(String image) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setMatrix(image);
			}
		}
	}

	/**
	 * Sets a l8 image from a color matrix.
	 * @param colorMatrix   Color matrix [y][x]
	 * @throws L8Exception  L8 generic exception.
	 */
	public void setMatrix(Color[][] colorMatrix) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setMatrix(colorMatrix);
			}
		}
	}
	/**
	 * Clears matrix, all leds to black.
	 * @throws L8Exception  L8 generic exception.
	 */
	public void clearMatrix() throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.clearMatrix();
			}
		}
	}

	/**
	 * Gets current color Matrix from a L8 connected device given by index.
	 * @param i   L8 index in the connected device list. start from 0 to N.
	 * @return    Color Matrix.
	 * @throws L8Exception  L8 generic exception.
	 */
	public Color[][] getMatrix(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getMatrix();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns Color Matrix from the given L8 device
	 * @param listener     Listener that receives the matrix.
	 * @param i            L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getMatrix(OnColorMatrixResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getMatrix(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Sets led to a specific Color.
	 * @param x            x position.
	 * @param y            y position.
	 * @param color        Color of the given led.
	 * @throws L8Exception L8 generic exception.
	 */
	public void setLED(int x, int y, Color color) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setLED(x, y, color);
			}
		}
	}

	/**
	 * Clears led
	 * @param x x position.
	 * @param y y position.
	 * @throws L8Exception L8 generic exception.
	 */
	public void clearLED(int x, int y) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.clearLED(x, y);
			}
		}
	}

	/**
	 * Gets current Color from a given led.
	 * @param x x position.
	 * @param y y position.
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return  Current color of a given led
	 * @throws  L8Exception L8 generic exception.
	 */
	public Color getLED(int x, int y, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getLED(x, y);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Gets current Color from a given led.
	 * @param x         x position.
	 * @param y         y position.
	 * @param listener   Listener that receives the led color value.
	 * @param i         L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8Exception L8 generic exception.
	 */
	public void getLED(int x, int y, OnColorResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getLED(x, y, listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Sets a superLed from image
	 * @param image       
	 * @throws L8Exception L8 generic exception.
	 */
	public void setSuperLED(String image) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setSuperLED(image);
			}
		}
	}

	/**
	 * Sets a superled with a given color.
	 * @param color        Color to set.
	 * @throws L8Exception L8 generic exception.
	 */
	public void setSuperLED(Color color) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setSuperLED(color);
			}
		}
	}

	/**
	 * Sets Super Led to black.
	 * @throws L8Exception L8 generic exception.
	 */
	public void clearSuperLED() throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.clearSuperLED();
			}
		}
	}

	/**
	 * Returns the color of super Led.
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return  Color value.
	 * @throws L8Exception L8 generic exception.
	 */
	public Color getSuperLED(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getSuperLED();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns the color of super Led.
	 * @param listener   Listener that receives color value. 
	 * @param i          L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getSuperLED(OnColorResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getSuperLED(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Enables a sensor of a given L8 device.
	 * @param sensor Sensor that is enabled.
	 * @param i      L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void enableSensor(Sensor sensor, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).enableSensor(sensor);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns sensor status of a given L8 device.
	 * @param sensor Sensor.
	 * @param i      L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public Sensor.Status getSensor(Sensor sensor, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getSensor(sensor);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns sensor status of a given L8 device.
	 * @param sensor   Sensor.   
	 * @param listener Listener that receives status sensor. 
	 * @param i        L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getSensor(Sensor sensor, OnSensorStatusResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getSensor(sensor, listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns sensor status of all sensors form a given L8 device.
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return List of all sensors.
	 * @throws L8Exception L8 generic exception.
	 */
	public List<Sensor.Status> getSensors(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getSensors();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns sensor status of all sensors form a given L8 device.
	 * @param listener     Listener that receives status sensor. 
	 * @param i            L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getSensors(OnSensorStatusListResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getSensors(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * disables a sensor of a given L8 device.
	 * @param sensor Sensor that is enabled.
	 * @param i      L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void disableSensor(Sensor sensor, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).disableSensor(sensor);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}
	
	/**
	 * Checks if a sensor is enabled from a given L8 device.
	 * @param sensor Sensor   
	 * @param i      L8 index in the connected device list. start from 0 to N.
	 * @return <code>true</code> if the sensor is enabled.
	 * @throws L8Exception L8 generic exception.
	 */
	public boolean getSensorEnabled(Sensor sensor, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getSensorEnabled(sensor);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Checks if a sensor is enabled from a given L8 device.
	 * @param sensor    Sensor   
	 * @param i         L8 index in the connected device list. start from 0 to N.
	 * @param listener  Listener that receives sensor status.
	 * @return <code>true</code> if the sensor is enabled.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getSensorEnabled(Sensor sensor, OnBooleanResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getSensorEnabled(sensor, listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}
    
	/**
	 * Returns if bluetooth is enabled.
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return <code>true</code> if bluetooth is enabled.
	 * @throws L8Exception L8 generic exception.
	 */
	public boolean getBluetoothEnabled(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getBluetoothEnabled();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns if bluetooth is enabled.
	 * @param i        L8 index in the connected device list. start from 0 to N.
	 * @param listener Listener that receives status.
	 * @return <code>true</code> if bluetooth is enabled.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getBluetoothEnabled(OnBooleanResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getBluetoothEnabled(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns battery status from a given L8 device. 
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return % of battery.
	 * @throws L8Exception L8 generic exception.
	 */
	public float getBatteryStatus(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getBatteryStatus();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns battery status from a given L8 device. 
	 * @param i        L8 index in the connected device list. start from 0 to N.
	 * @param listener Listener that receives battery status.
	 * @return % of battery.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getBatteryStatus(OnFloatResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getBatteryStatus(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public int getButton(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getButton();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public void getButton(OnIntegerResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getButton(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public int getMemorySize(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getMemorySize();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public void getMemorySize(OnIntegerResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getMemorySize(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public int getFreeMemory(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getFreeMemory();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public void getFreeMemory(OnIntegerResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getFreeMemory(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public String getID(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getID();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	public void getID(OnStringResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getID(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns the firmware version of a given L8.
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return Firmware version.
	 * @throws L8Exception L8 generic exception.
	 */
	public Version getVersion(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getVersion();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns the firmware version of a given L8.
	 * @param listener Listener that receives firmware version.
	 * @param i        L8 index in the connected device list. start from 0 to N.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getVersion(OnVersionResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getVersion(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Sets animation from a jsonArray.
	 * @param jsonFrames  JSONArray with N frames.
	 * 					  format exmple:
	 * 					  <p>
	 * 						[{"duration":"2","image":"#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#000000-#00ffff-#00ffff-#000000-#000000-#000000-#00ffff-#ffffff-#000000-#ffffff-#ffffff-#000000-#ffffff-#000000-#ffffff-#ffffff-#000000-#ffffff-#ffffff-#000000-#ffffff-#000000-#ffffff-#00ffff-#000000-#00ffff-#00ffff-#000000-#000000-#000000-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#00ffff-#ff0000"},{N1},{N2}...,{N}]
	 * 					  </p>	
	 * @throws L8Exception L8 generic exception.
	 */
	public void setAnimation(JSONArray jsonFrames) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setAnimation(jsonFrames);
			}
		}
	}

	/**
	 * Sets animations from Animation. This is the most easy way of setting animations.
	 * @param animation    Animation object.
	 * @throws L8Exception L8 generic exception.
	 */
	public void setAnimation(Animation animation) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setAnimation(animation);
			}
		}
	}

	/**
	 * Returns a string with URL or Mac from a given L8.
	 * @param i L8 index in the connected device list. start from 0 to N.
	 * @return URL if L8 is a simulator and a Bluetooth mac if is real device.
	 * @throws L8Exception L8 generic exception.
	 */
	public String getConnectionURL(int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			return connectedDevices.get(i).getConnectionURL();
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/**
	 * Returns a string with URL or Mac from a given L8.
	 * @param i        L8 index in the connected device list. start from 0 to N.
	 * @param listener Listener that receives URL.
	 * @return URL if L8 is a simulator and a Bluetooth mac if it is real device.
	 * @throws L8Exception L8 generic exception.
	 */
	public void getConnectionURL(OnStringResultListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).getConnectionURL(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	// /nuevos metodos para notificaciones y para run l8 apps y brillo
	/**
	 * Stops any application that is running in L8 device.
	 * @throws L8Exception L8 generic exception.
	 */
	public void stopCurrentL8app() throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.stopCurrentL8app();
			}
		}
	}

	/**
	 * Runs dice app.
	 * @param color Color of dice.   
	 * @throws L8Exception  L8 generic exception.
	 */
	public void runL8AppDice(Color color) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.runL8AppDice(color);
			}
		}
	}

	/**
	 * Sets bright level
	 * @param Brightlevel The bright level value can be 0 high, 1 medium or 2 low.
	 * @throws L8Exception L8 generic exception.
	 */
	public void setL8Brightness(int Brightlevel) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setL8Brightness(Brightlevel);
			}
		}
	}

	public void onNotification(String bundle, int eventNotificationID, int categoryNotificationID) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.onNotification(bundle, eventNotificationID, categoryNotificationID);
			}
		}
	}

	/**
	 * Displays text in L8 device.
	 * @param text  Text to display.
	 * @param loop  True if text is shown in loop mode
	 * @param color Text color
	 * @param speed Speed of text displacement, can be 0 high , 1 normal or 2 low. 
	 * @throws L8Exception L8 generic exception.
	 */
	public void setText(String text, int loop, Color color, int speed) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.setText(text, loop, color, speed);
			}
		}
	}

	/**
	 * Runs proximity and luminosity apps
	 * @param sensor       Selects the sensor type, 0 for proximity sensor and 1 for luminosity sensor.
	 * @param colorMatrix  Color Matrix, the color of all L8 leds.
	 * @param colorBackLed Color of back led.
	 * @param threshold    Config the threshold, recommend values: 
	 * 					   0x32 for proximity and 0x02 for liminosity
	 * @throws L8Exception L8 generic exception.
	 */
	public void runL8AppLuminosityAndProximity(int sensor, Color colorMatrix, Color colorBackLed, byte threshold) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.runL8AppLuminosityAndProximity(sensor, colorMatrix, colorBackLed, threshold);
			}
		}
	}

	/**
	 * Runs light color app
	 * @param lightColorMode  Selects the light color mode: 1 multicolor, 2 tropical, 3 galaxy, 4 aurora. 
	 * @param speed           Speed of the color sequence in milliseconds.
	 * @param backLedInverted 1 inverted color and 0 normal color.
	 * @throws L8Exception L8 generic exception.
	 */
	public void runL8AppLights(int lightColorMode, int speed, int backLedInverted) throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.runL8AppLights(lightColorMode, speed, backLedInverted);
			}
		}
	}

	/**
	 * Runs party app. This app active color matrix based on noise level.
	 * @throws L8Exception L8 generic exception.
	 */
	public void runL8AppPartyMode() throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.runL8AppPartyMode();
			}
		}
	}

	/**
	 * Shuts down all L8 devices connected and selected.
	 * @throws L8Exception L8 generic exception.
	 */
	public void shutDown() throws L8Exception {
		for (AndroidBluetoothL8 connectedL8 : connectedDevices) {
			if (connectedL8.isSelected()) {
				connectedL8.shutDown();
			}
		}
	}

	/**
	 * Sets a listener that receives event from L8 device.
	 * @param listener Listener to set.
	 * @param i        L8 index in the connected device list. start from 0 to N.
	 * @see 		   OnEventListener
	 * @throws L8Exception L8 generic exception.
	 */
	public void setEventListener(OnEventListener listener, int i) throws L8Exception {
		if (i < connectedDevices.size()) {
			connectedDevices.get(i).setEventListener(listener);
		} else {
			throw new L8Exception("Out of index: l8 device index");
		}
	}

	/*
	 * public void requestSimulator(){ AsyncTask<Void,Void,Void> taskSimulator =
	 * new AsyncTask<Void,Void,Void>(){
	 * 
	 * @Override protected Void doInBackground(Void... params) { try{ String
	 * lastEmulatorId = preferences.getLastConnectedEmulator(); L8 lastEmulator
	 * = null; if (lastEmulatorId != null) { lastEmulator =
	 * reconnectSimulator(lastEmulatorId); } if(lastEmulator!=null){ for
	 * (AndroidL8ManagerListener l : listeners) {
	 * l.simulatorRequested(lastEmulator,false); } }else{ AndroidRESTfulL8 l8 =
	 * new AndroidRESTfulL8(); l8.createSimulator(); lastEmulatorId =
	 * l8.getID(); preferences.setLastConnectedEmulator(lastEmulatorId); for
	 * (AndroidL8ManagerListener l : listeners) { l.simulatorRequested(l8,true);
	 * } } }catch(Exception e){ for (AndroidL8ManagerListener l : listeners) {
	 * l.simulatorRequested(null,false); } } return null; }};
	 * taskSimulator.execute(); }
	 */

	/**
	 * You can request a simulator for test purpose. It is an asynchronous request.
	 */
	public void requestSimulator() {
		if (!simulatorLoaded) {
			AsyncTask<Void, Void, Void> taskSimulator = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					try {
						String lastEmulatorId = preferences.getLastConnectedEmulator();
						L8 lastEmulator = null;
						if (lastEmulatorId != null) {
							lastEmulator = reconnectSimulator(lastEmulatorId);
						}
						if (lastEmulator != null) {
							simulator = lastEmulator;
							simulatorLoaded = true;
							Handler mHandler = new Handler(Looper.getMainLooper());
							mHandler.post(new Runnable() {

								@Override
								public void run() {
									for (AndroidL8ManagerListener l : listeners) {
										l.simulatorRequested(simulator, false);
									}
								}});
						} else {
							final AndroidRESTfulL8 l8 = new AndroidRESTfulL8();
							l8.createSimulator();
							lastEmulatorId = l8.getID();
							preferences.setLastConnectedEmulator(lastEmulatorId);
							simulator = l8;
							simulatorLoaded = true;
							Handler mHandler = new Handler(Looper.getMainLooper());
							mHandler.post(new Runnable() {

								@Override
								public void run() {
									for (AndroidL8ManagerListener l : listeners) {
										l.simulatorRequested(l8, true);
									}
								}

							});

						}
					} catch (Exception e) {
						Log.e("Simulator", "Error: " + e);
						Handler mHandler = new Handler(Looper.getMainLooper());
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								for (AndroidL8ManagerListener l : listeners) {
									l.simulatorRequested(null, false);
								}
							}
						});
					}
					return null;
				}
			};
			taskSimulator.execute();
		} else {
			for (AndroidL8ManagerListener l : listeners) {
				l.simulatorRequested(simulator, false);
			}
		}
	}

	private L8 reconnectSimulator(String lastEmulatorId) throws L8Exception {
		AndroidRESTfulL8 l8 = new AndroidRESTfulL8();
		return l8.reconnectSimulator(lastEmulatorId);
	}

	/**
	 * Returns a loaded simulator. 
	 * @return L8 simulator.
	 */
	public L8 getSimulatorLoaded() {
		return simulator;
	}

	/**
	 * Checks if a simulator has been loaded previously.
	 * @return <code>true</code> if simulator is loaded and ready to use.
	 */
	public boolean isSimulatorLoaded() {
		return simulatorLoaded;
	}

	/**
	 * Returns the number of L8 device connected.
	 * @return L8 device connected.
	 */
	public int getConnectedDevicesCount() {
		return connectedDevices.size();
	}

}
