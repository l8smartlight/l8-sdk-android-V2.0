package com.l8smartlight.sdk.core;

import java.util.List;

import org.json.simple.JSONArray;


public interface L8 
{
	
	public static final int EVENT_POWER_BUTTON_PRESSED = 0;
	public static final int EVENT_LIMINOSITY_LEVEL_UP = 1;
	public static final int EVENT_ROTATION = 2;
	
	public static final int EVENT_LIMINOSITY_LEVEL_DOWN = 3;
	
	public static final int EVENT_PROXIMITY_LEVEL_DOWN = 4;
	public static final int EVENT_PROXIMITY_LEVEL_UP = 5;
	
	
	public enum ConnectionType 
	{
		Bluetooth, USB, RESTful
	}	
	
	public ConnectionType getConnectionType();
	
	public static class Version
	{
		private int hardware;
		private int software;
		private int firmVersion0,firmVersion1,firmVersion2;
		
		public int getHardware() 
		{
			return hardware;
		}
		
		public void setHardware(int hardware) 
		{
			this.hardware = hardware;
		}
		
		public int getFirmVersion0() {
			return firmVersion0;
		}

		public Version(){
			
		}
		
		public Version(int hardware,int software){
			this.hardware = hardware;
			this.software = software;
		}
		
		public void setFirmVersion0(int firmVersion0) {
			this.firmVersion0 = firmVersion0;
		}

		public int getFirmVersion1() {
			return firmVersion1;
		}

		public void setFirmVersion1(int firmVersion1) {
			this.firmVersion1 = firmVersion1;
		}

		public int getFirmVersion2() {
			return firmVersion2;
		}

		public void setFirmVersion2(int firmVersion2) {
			this.firmVersion2 = firmVersion2;
		}
		
		public int getSoftware() {
			return software;
		}

		public void setSoftware(int software) {
			this.software = software;
		}

		public String toString()
		{
			return "{hardware: " + hardware + ", software: " + firmVersion0 +"." +firmVersion1+"."+firmVersion2+" }";
		}
		
		
	}	
	
	public static class Frame
	{
		protected Color[][] matrix;
		protected int duration;
		protected Color backLed;
		
		public Color getBackLed() {
			return backLed;
		}

		public void setBackLed(Color backLed) {
			this.backLed = backLed;
		}

		public Color[][] getMatrix() 
		{
			return matrix;
		}
		
		public void setMatrix(Color[][] matrix) 
		{
			this.matrix = matrix;
		}
		
		public int getDuration() 
		{
			return duration;
		}
		
		public void setDuration(int duration) 
		{
			this.duration = duration;
		}
		
		public Frame(Color[][] matrix, int duration)
		{
			this.matrix = matrix;
			this.duration = duration;
		}
		
		public Frame(Color[][] matrix, Color backLed, int duration)
		{
			this.matrix = matrix;
			this.backLed = backLed;
			this.duration = duration;
		}
		
		protected String join(Color[] array, String separator) {
		    StringBuilder sb = new StringBuilder();
		    for (int i = 0, il = array.length; i < il; i++) {
		        if (i > 0) {
		            sb.append(separator);
		        }
		        sb.append(Color.encode(array[i]));
		    }
		    return sb.toString();
		}
		
		protected String join(String[] array, String separator) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, il = array.length; i < il; i++) {
				if (i > 0) {
					sb.append(separator);
				}
				sb.append(array[i]);
			}
			return sb.toString();
		}		
		
		public String encodeImage() {
			String[] rows = new String[matrix.length];
			for (int i = 0; i < matrix.length; i++) {
				rows[i] = join(matrix[i], "-");
			}
			String front = join(rows, "-");
			return front + "-" + Color.encode(backLed);
		}
		
	}
	
	public static class Animation
	{
		protected List<Frame> frames;

		public List<Frame> getFrames() 
		{
			return frames;
		}

		public void setFrames(List<Frame> frames) 
		{
			this.frames = frames;
		}
		
		public Animation(List<Frame> frames)
		{
			this.frames = frames;
		}
	}
	
	public static interface OnResultListener 
	{
		public void onResult(Object result);
	}
	
	public static interface OnIntegerResultListener 
	{
		public void onResult(int result);
	}
	
	public static interface OnFloatResultListener 
	{
		public void onResult(float result);
	}
	
	public static interface OnBooleanResultListener 
	{
		public void onResult(boolean result);
	}
	
	public static interface OnColorMatrixResultListener 
	{
		public void onResult(Color[][] result);
	}
	
	public static interface OnColorResultListener 
	{
		public void onResult(Color result);
	}
	
	public static interface OnSensorStatusResultListener 
	{
		public void onResult(Sensor.Status result);
	}
	
	public static interface OnSensorStatusListResultListener 
	{
		public void onResult(List<Sensor.Status> result);
	}
	
	public static interface OnStringResultListener 
	{
		public void onResult(String result);
	}
	
	public static interface OnVersionResultListener 
	{
		public void onResult(Version result);
	}
	
	public static interface OnEventListener 
	{
		/**
		 * Receives event from L8 device
		 * @param eventId  Event type: EVENT_POWER_BUTTON_PRESSED, EVENT_LIMINOSITY_LEVEL_UP, EVENT_ROTATION, EVENT_LIMINOSITY_LEVEL_DOWN, EVENT_PROXIMITY_LEVEL_DOWN and EVENT_PROXIMITY_LEVEL_UP  
		 * 					
		 * @param message Extra data from each event.
		 * @param l8      L8 device that sends events.
		 */
		public void onEvent(int eventId,String message,L8 l8);
	}
	
	
	public void setMatrix(String image) throws L8Exception;
	public void setMatrix(Color[][] colorMatrix) throws L8Exception;
		
	public void clearMatrix() throws L8Exception;

	public Color[][] getMatrix() throws L8Exception;
	public void getMatrix(OnColorMatrixResultListener listener) throws L8Exception;
	
	public void setLED(int x, int y, Color color) throws L8Exception;
	
	public void clearLED(int x, int y) throws L8Exception;

	public Color getLED(int x, int y) throws L8Exception;
	public void getLED(int x, int y, OnColorResultListener listener) throws L8Exception;
	
	public void setSuperLED(String image) throws L8Exception;
	public void setSuperLED(Color color) throws L8Exception;
	
	public void clearSuperLED() throws L8Exception;
	
	public Color getSuperLED() throws L8Exception;
	public void getSuperLED(OnColorResultListener listener) throws L8Exception;
	
	public void enableSensor(Sensor sensor) throws L8Exception;

	public Sensor.Status getSensor(Sensor sensor) throws L8Exception;	
	public void getSensor(Sensor sensor, OnSensorStatusResultListener listener) throws L8Exception;
	
	public List<Sensor.Status> getSensors() throws L8Exception;
	public void getSensors(OnSensorStatusListResultListener listener) throws L8Exception;
	
	public void disableSensor(Sensor sensor) throws L8Exception;
	
	public boolean getSensorEnabled(Sensor sensor) throws L8Exception;
	public void getSensorEnabled(Sensor sensor, OnBooleanResultListener listener) throws L8Exception;
	
	public boolean getBluetoothEnabled() throws L8Exception;
	public void getBluetoothEnabled(OnBooleanResultListener listener) throws L8Exception;
	
	public float getBatteryStatus() throws L8Exception;
	public void getBatteryStatus(OnFloatResultListener listener) throws L8Exception;

	public int getButton() throws L8Exception;
	public void getButton(OnIntegerResultListener listener) throws L8Exception;
	
	public int getMemorySize() throws L8Exception;
	public void getMemorySize(OnIntegerResultListener listener) throws L8Exception;

	public int getFreeMemory() throws L8Exception;
	public void getFreeMemory(OnIntegerResultListener listener) throws L8Exception;
	
	public String getID() throws L8Exception;
	public void getID(OnStringResultListener listener) throws L8Exception;
	
	public Version getVersion() throws L8Exception;
	public void getVersion(OnVersionResultListener listener) throws L8Exception;
	
	public void setAnimation(JSONArray jsonFrames) throws L8Exception;	
	public void setAnimation(Animation animation) throws L8Exception;
	
	public String getConnectionURL() throws L8Exception;
	public void getConnectionURL(OnStringResultListener listener) throws L8Exception;
	
	
	///nuevos metodos para notificaciones  y para run l8 apps y brillo
	
	public void stopCurrentL8app () throws L8Exception ;
	public void runL8AppDice (Color color) throws L8Exception;
	public void setL8Brightness (int Brightlevel) throws L8Exception;
	
	
	public void onNotification(String bundle, int eventNotificationID, int categoryNotificationID) throws L8Exception ;
	
	public void setText (String text, int loop, Color color, int speed)throws L8Exception;
	public void runL8AppLuminosityAndProximity (int sensor, Color colorMatrix,Color colorBackLed, byte threshold)throws L8Exception ;
	public void runL8AppLights (int lightColorMode,int speed,int backLedInverted)throws L8Exception ;
	public void runL8AppPartyMode ()throws L8Exception ;
	public void shutDown ()throws L8Exception ;
	
	public void setEventListener(OnEventListener listener);
	//Run app 1 (light mode). Colors=1 (RGB). Speed=30 (300ms). BackLed=0 (not inverted)
	/**
	 * Checks if this L8 device is selected.
	 * @return
	 */
	public boolean isSelected();
	/**
	 * Selects this L8 device to operate through AndroidL8Manager. All L8 devices are selected by default. 
	 * @param selected Sets if this L8 is selected or not.
	 */
	public void setSelected(boolean selected);
	
	
}
