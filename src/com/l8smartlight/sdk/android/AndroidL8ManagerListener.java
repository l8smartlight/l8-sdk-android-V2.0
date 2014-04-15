package com.l8smartlight.sdk.android;

import com.l8smartlight.sdk.core.L8;

/**
 * You have to implement this interface if you want to receive events from AndroidL8Manager. 
 * 
 * 
 * @author     smartlight inc
 *
 */
public interface AndroidL8ManagerListener {

	/**
	 * Bluetooth is not available in your device
	 */
	public void bluetoothNotAvailable();
	
	/**
	 * Bluetooth is not active in your device.
	 */
	public void bluetoothNotEnabled();
	
	/**
	 * there are not registered L8 devices.
	 */
	public void noDevicesRegistered();
	
	/**
	 * A device has been connected.
	 *@param l8    L8 device
	 */
	public void deviceConnected(L8 l8);
	
	/**
	 * Connection lost with L8.
	 *@param l8    L8 device
	 */
	public void deviceDisconnected(L8 l8);
	
	/**
	 * Your device is not connected with any l8 device.
	 */
	public void noDevicesConnected();
	
	/**
	 * Connection process has finished with registered L8 devices. At least one L8 is connected to you.
	 */
	public void allConnectionsDone();
	
	/**
	 * AndroidL8Manager provides a simulator previously requested by user. 
	 *
	 *@param l8               L8 virtual device.
	 *@param newSimulator     newSimulator is true if has not been created before.
	 */
	public void simulatorRequested(L8 l8,boolean newSimulator);
}
