package com.l8smartlight.sdk.android.bluetooth;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;

import com.l8smartlight.sdk.android.Util;
import com.l8smartlight.sdk.base.NonBlockingL8;
import com.l8smartlight.sdk.core.Color;
import com.l8smartlight.sdk.core.L8;
import com.l8smartlight.sdk.core.L8Exception;
import com.l8smartlight.sdk.core.L8MethodNotSupportedException;
import com.l8smartlight.sdk.core.Sensor;
import com.l8smartlight.sdk.core.Sensor.Status;

public class AndroidBluetoothL8 extends NonBlockingL8 implements  L8 {
	
	public static final int NUM_ROWS	= 8;
	public static final int NUM_COLUMNS	= 8;
	
	public enum L8Mode 
	{
		L8_MODE_4BIT,	//Default mode
		L8_MODE_8BIT
	}

	protected BluetoothClient bluetoothClient;
	protected L8Mode mode;
	
	private OnEventListener onEventListener = null;
	private OnVersionResultListener onVersionResultListener = null;
	
	
	public AndroidBluetoothL8(BluetoothClient bluetoothClient) 
	{
		this.bluetoothClient = bluetoothClient;
		this.mode = L8Mode.L8_MODE_4BIT;
	}	
	
	public boolean send(byte[] buffer) 
	{
		try {
			if (bluetoothClient != null) {
				// Check that we're actually connected before trying anything
	            if (bluetoothClient.getState() != BluetoothClient.STATE_CONNECTED) {
	                return false;
	            }
	            // Check that there's actually something to send
	            if (buffer != null && buffer.length > 0) {
	            	Util.error("BYTES WRITE: " + buffer.length + ": " + Util.bytesToHex(buffer.length, buffer));
	            	bluetoothClient.write(buffer);
	                return true;
	            }
	    	}
		} catch(Exception ignored) {}
		return false;
	}
	
	protected L8.OnFloatResultListener readBatteryListener;
	protected L8.OnSensorStatusResultListener readTemperatureListener;
	protected L8.OnSensorStatusResultListener readAccelerationListener;
	protected L8.OnSensorStatusResultListener readAmbientLightListener;
	protected L8.OnSensorStatusResultListener readProximityListener;
	protected L8.OnSensorStatusResultListener readNoiseListener;
	
	private boolean selected = true;
	
	protected int readTwoBytesInt(byte[] buffer) {
		byte[] v = new byte[2];
    	v[0] = buffer[4];
    	v[1] = buffer[5];
		ByteBuffer bb = ByteBuffer.wrap(v);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort() & 0xffff; // para interpretar como unsigned short.
	}
	
	protected int readOneByteInt(byte buffer) {
		byte[] v = new byte[2];
    	v[0] = 0;
    	v[1] = buffer;
		ByteBuffer bb = ByteBuffer.wrap(v);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort() & 0xffff; // para interpretar como unsigned short.
	}
	
	public void received(int bytes, byte[] buffer) 
	{
		// TODO: Comentar:
		Util.error("BYTES READ: " + bytes + ": " + Util.bytesToHex(bytes, buffer));
		
        if (bytes > 1) {
        	byte code = buffer[3];
        	if (code == RLPCommand.READ_BATTERY_RESULT && readBatteryListener != null) {
        	//	int result = readTwoBytesInt(buffer);
            //	float batteryVoltage = (float)result / 1000;
            	float batteryVoltage = readOneByteInt(buffer[6]);
        		readBatteryListener.onResult(batteryVoltage);
        	}
        	if (code == RLPCommand.READ_TEMPERATURE_RESULT && readTemperatureListener != null) {
        		int result = readTwoBytesInt(buffer);
            	float celsiusValue = (float)result / 10;
            	float fahrenheitValue = celsiusValue * 9.0f/5.0f + 32.0f;
            	readTemperatureListener.onResult(new Sensor.TemperatureStatus(true, celsiusValue, fahrenheitValue));
        	}
        	if (code == RLPCommand.READ_ACCELERATION_RESULT && readAccelerationListener != null) {
            	int accX = readOneByteInt(buffer[4]);
            	int accY = readOneByteInt(buffer[5]);
            	int accZ = readOneByteInt(buffer[6]);
            	int lying = readOneByteInt(buffer[7]);
            	int orientation = readOneByteInt(buffer[8]);
            	int tap = readOneByteInt(buffer[9]);
            	int shake = readOneByteInt(buffer[10]);
            	readAccelerationListener.onResult(new Sensor.AccelerationStatus(true, accX, accY, accZ, shake, orientation, tap, lying));
        	}
        	if (code == RLPCommand.READ_AMBIENTLIGHT_RESULT && readAmbientLightListener != null) {
        	//	int ambientlight = readTwoBytesInt(buffer);
        		int ambientlight = readOneByteInt(buffer[6]);
        		readAmbientLightListener.onResult(new Sensor.AmbientLightStatus(true, ambientlight));
        	}
        	if (code == RLPCommand.READ_PROXIMITY_RESULT && readProximityListener != null) {
        	//	int proximity = readTwoBytesInt(buffer);
        		int proximity = readOneByteInt(buffer[6]);
        		readProximityListener.onResult(new Sensor.ProximityStatus(true, proximity));
        	}
        	if (code == RLPCommand.READ_NOISE_RESULT && readNoiseListener != null) {
        		int noise = readTwoBytesInt(buffer);
        	//	int noise = readOneByteInt(buffer[6]);
        		readNoiseListener.onResult(new Sensor.NoiseStatus(true, noise));
        	}
        	if(code == RLPCommand.READ_BUTTON_PRESSED && onEventListener!=null){
        		onEventListener.onEvent(L8.EVENT_POWER_BUTTON_PRESSED,"",this);
        	}
        	if(code == RLPCommand.READ_CHANGE_ORIENTATION && onEventListener!=null){
        		onEventListener.onEvent(L8.EVENT_ROTATION,"",this);
        	}
        	if(code == RLPCommand.READ_AMBIENTLIGHT_RESULT && onEventListener!=null){
        		int ambientlight = readOneByteInt(buffer[6]);
        		if(ambientlight<20){
        			onEventListener.onEvent(L8.EVENT_LIMINOSITY_LEVEL_DOWN,ambientlight+"%",this);
        		}else{
        			onEventListener.onEvent(L8.EVENT_LIMINOSITY_LEVEL_UP,ambientlight+"%",this);	
        		}
        	}
        	if(code == RLPCommand.READ_PROXIMITY_RESULT && onEventListener!=null){
        		int proximity = readOneByteInt(buffer[6]);
        		if(proximity<20){
        			onEventListener.onEvent(L8.EVENT_PROXIMITY_LEVEL_DOWN,proximity+"%",this);
        		}else{
        			onEventListener.onEvent(L8.EVENT_PROXIMITY_LEVEL_UP,proximity+"%",this);	
        		}
        	}
        	if(code == RLPCommand.CMD_READ_VERSIONS && this.onVersionResultListener!=null){
        		int firmware0 = readOneByteInt(buffer[4]);
        		int firmware1 = readOneByteInt(buffer[5]);
        		int firmware2 = readOneByteInt(buffer[6]);
        		Version version = new Version();
        		version.setFirmVersion0(firmware0);
        		version.setFirmVersion1(firmware1);
        		version.setFirmVersion2(firmware2);
        		onVersionResultListener.onResult(version);
        	}
        }
	}

	@Override
	public ConnectionType getConnectionType() 
	{
		return ConnectionType.Bluetooth;
	}
	
	public void setMode(L8Mode mode)
	{
		this.mode = mode;
	}
	
	@Override
	public void setMatrix(Color[][] colorMatrix) throws L8Exception 
	{
		stopCurrentAnimation();
		send(RLPCommand.BuildMatrixSet(colorMatrix, NUM_ROWS, NUM_COLUMNS, mode));
	}

	@Override
	public void clearMatrix() throws L8Exception 
	{
		stopCurrentAnimation();
		send(RLPCommand.BuildMatrixClear());
	}
	
	@Override
	public void setLED(int x, int y, Color color) throws L8Exception 
	{
		stopCurrentAnimation();
		send(RLPCommand.BuildLedSet((byte)x, (byte)y, color, mode));
	}

	@Override
	public void clearLED(int x, int y) throws L8Exception 
	{
		stopCurrentAnimation();
		System.out.println("bluetooth::clearLED");
	}

	@Override
	public void setSuperLED(Color color) throws L8Exception {
		stopCurrentAnimation();
		send(RLPCommand.BuildBackledSet(color, mode));
	}
	
	@Override
	public void clearSuperLED() throws L8Exception {
		stopCurrentAnimation();
		System.out.println("bluetooth::clearSuperLED");
	}	
	
	@Override
	public void enableSensor(Sensor sensor) throws L8Exception 
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void disableSensor(Sensor sensor) throws L8Exception
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void getBatteryStatus(L8.OnFloatResultListener listener) throws L8Exception {
		readBatteryListener = listener;
		send(RLPCommand.BuildReadBattery());
	}
	
	///a�adiendo metodos para brillo y set notifications
	
	public void setNotificacion(String bundle, int idCategory, int idTypeNotification){
		
		
		
	
	}
	
	public void setL8BrightHight() {
		
	}
	
	public void setL8BrightLow (){
		
	}
	@Override
	public void stopCurrentL8app () throws L8Exception {
		
		send(RLPCommand.BuildStopCurrentL8App());
	}

	protected L8.Animation currentAnimation;
    protected int currentAnimationIndex = 0;	
    protected boolean shouldStopAnimation = true;
	protected Thread currentAnimationThread;
    
    protected void stopCurrentAnimation() 
    {
    	if (shouldStopAnimation && currentAnimationThread != null && currentAnimationThread.isAlive()) {
    		currentAnimationThread.interrupt();
    	}
    }
    
    protected void startCurrentAnimation(L8.Animation animation) 
    {
    	stopCurrentAnimation();
    	currentAnimation = animation;
    	currentAnimationIndex = 0;
    	currentAnimationThread = new Thread()
	    {
	        @Override
	        public void run() {
	    		try {
		        	while (true) {
		        		List<L8.Frame> frames = currentAnimation.getFrames();
		        		if (currentAnimationIndex < frames.size()) {
		        			L8.Frame currentFrame = frames.get(currentAnimationIndex);
		        			shouldStopAnimation = false;
		        			setMatrix(currentFrame.getMatrix());
		        			setSuperLED(currentFrame.getBackLed());
		        			shouldStopAnimation = true;
		        			currentAnimationIndex++;
		        			if (currentAnimationIndex > frames.size() - 1) currentAnimationIndex = 0;
		        			sleep(currentFrame.getDuration());
		        		}
		        	}
	    		} catch (InterruptedException e) {
	    			return;
	    		} catch (L8Exception e) {
	    			return;
	    		}
	        }
	    };    	
	    currentAnimationThread.start();
    }
	
	@Override
	public void setAnimation(L8.Animation animation) throws L8Exception
	{
		startCurrentAnimation(animation);
	}
	
	@Override
	public String getConnectionURL() throws L8Exception 
	{
		if (bluetoothClient != null && bluetoothClient.getConnectedDevice() != null) {
			return bluetoothClient.getConnectedDevice().getAddress();
		} else {
			return this.getMac();
		}
	}

	@Override
	public void getBluetoothEnabled(OnBooleanResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getButton(OnIntegerResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getConnectionURL(OnStringResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getFreeMemory(OnIntegerResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getID(OnStringResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getLED(int x, int y, OnColorResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getMatrix(OnColorMatrixResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getMemorySize(OnIntegerResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getSensor(Sensor sensor, OnSensorStatusResultListener listener) throws L8Exception {
		if (sensor.equals(Sensor.TEMPERATURE)) {
			readTemperatureListener = listener;
			send(RLPCommand.BuildReadTemperature());
		} else if (sensor.equals(Sensor.ACCELERATION)) {
			readAccelerationListener = listener;
			send(RLPCommand.BuildReadAcceleration());
		} else if (sensor.equals(Sensor.AMBIENTLIGHT)) {
			readAmbientLightListener = listener;
			send(RLPCommand.BuildReadAmbientLight());
		} else if (sensor.equals(Sensor.PROXIMITY)) {
			readProximityListener = listener;
			send(RLPCommand.BuildReadProximity());
		} else if (sensor.equals(Sensor.NOISE)) {
			readNoiseListener = listener;
			send(RLPCommand.BuildReadNoise());
		}
	}

	@Override
	public void getSensorEnabled(Sensor sensor, OnBooleanResultListener listener) throws L8Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void getSensors(final OnSensorStatusListResultListener listener) throws L8Exception {
		final List<Sensor.Status> statuses = new ArrayList<Sensor.Status>();
		getSensor(Sensor.TEMPERATURE, new OnSensorStatusResultListener() {
			@Override
			public void onResult(Status temperature) {
				statuses.add(temperature);
				try {
					getSensor(Sensor.ACCELERATION, new OnSensorStatusResultListener() {
						@Override
						public void onResult(Status acceleration) {
							statuses.add(acceleration);
							try {
								getSensor(Sensor.AMBIENTLIGHT, new OnSensorStatusResultListener() {
									@Override
									public void onResult(Status ambientlight) {
										statuses.add(ambientlight);
										try {
											getSensor(Sensor.PROXIMITY, new OnSensorStatusResultListener() {
												@Override
												public void onResult(Status proximity) {
													statuses.add(proximity);
													try {
														getSensor(Sensor.NOISE, new OnSensorStatusResultListener() {
															@Override
															public void onResult(Status noise) {
																statuses.add(noise);
																
																if (listener != null) {
																	listener.onResult(statuses);
																	return;
																}
																
															}
														});
													} catch (L8Exception ignored) {}													
												}
											});
										} catch (L8Exception ignored) {}										
									}
								});
							} catch (L8Exception ignored) {}
						}
					});
				} catch (L8Exception ignored) {}
			}
		});
	}

	@Override
	public void getSuperLED(OnColorResultListener listener) throws L8Exception {
		throw new L8MethodNotSupportedException();
	}

	@Override
	public void getVersion(OnVersionResultListener listener) throws L8Exception {
		onVersionResultListener = listener;
		send(RLPCommand.BuildGetVersion());
	}

	@Override
	public void setL8Brightness(int Brightlevel) throws L8Exception {
		
		send(RLPCommand.BuildBrightLevel((byte)Brightlevel));
		
	}

	@Override
	public void onNotification(String bundle, int eventNotificationID,
			int categoryNotificationID) throws L8Exception {
		send(RLPCommand.BuildNotificationPosted( bundle, (byte)eventNotificationID,(byte)categoryNotificationID ));
		
	}

	@Override
	public void setText(final String text, final int loop, final Color color, final int speed)
			throws L8Exception {
		// TODO Auto-generated method stub
		
	//	stopCurrentL8app ();
	//	send(RLPCommand.BuildTexttoL8( text, (byte)loop,color,(byte)speed,mode ));
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				try{
				stopCurrentL8app();
				Thread.sleep(200);
				send(RLPCommand.BuildTexttoL8( text, (byte)loop,color,(byte)speed,mode ));
				}catch(Exception e){
					Log.e("l8bt", "Error: "+e);
				}
				return null;
			}
			
		};
		task.execute();
	}

	@Override
	public void runL8AppLuminosityAndProximity(final int sensor, final Color colorMatrix,
			final Color colorBackLed, final byte threshold) throws L8Exception {
		// TODO Auto-generated method stub
		//stopCurrentL8app ();
	//	send(RLPCommand.BuildRunL8AppLuminosityAndProximity((byte) sensor, colorMatrix, colorBackLed,threshold,mode ));
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				try{
				stopCurrentL8app();
				Thread.sleep(200);
				send(RLPCommand.BuildRunL8AppLuminosityAndProximity((byte) sensor, colorMatrix, colorBackLed,threshold,mode ));
				}catch(Exception e){
					Log.e("l8bt", "Error: "+e);
				}
				return null;
			}
			
		};
		task.execute();
	}

	@Override
	public void runL8AppLights(final int lightColorMode, final int speed,
			final int backLedInverted) throws L8Exception {
		// TODO Auto-generated method stub
	//	stopCurrentL8app ();
	//	send(RLPCommand.BuildRunL8AppLight((byte) lightColorMode, (byte) speed, (byte) backLedInverted ));
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				try{
				stopCurrentL8app();
				Thread.sleep(200);
				send(RLPCommand.BuildRunL8AppLight((byte) lightColorMode, (byte) speed, (byte) backLedInverted ));
				}catch(Exception e){
					Log.e("l8bt", "Error: "+e);
				}
				return null;
			}
			
		};
		task.execute();
	}
	@Override
	public void runL8AppDice (final Color color) throws L8Exception {
	//	stopCurrentL8app ();
		//System.out.println ( "antes de enviar command de run dice");
	//	send(RLPCommand.BuildRunL8appDice( color, mode));
		//System.out.println ( "comando enviado run dice");
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				try{
				stopCurrentL8app();
				Thread.sleep(200);
				send(RLPCommand.BuildRunL8appDice(color, mode));
				}catch(Exception e){
					Log.e("l8bt", "Error: "+e);
				}
				return null;
			}
			
		};
		task.execute();
	}
	
	///////////////

	@Override
	public void runL8AppPartyMode() throws L8Exception {
		// TODO Auto-generated method stub
	//	stopCurrentL8app ();
	//	send(RLPCommand.BuildRunL8appPartyMode());
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				try{
				stopCurrentL8app();
				Thread.sleep(200);
				send(RLPCommand.BuildRunL8appPartyMode());
				}catch(Exception e){
					Log.e("l8bt", "Error: "+e);
				}
				return null;
			}
			
		};
		task.execute();
	}

	@Override
	public void shutDown() throws L8Exception {
		// TODO Auto-generated method stub
		send(RLPCommand.BuildShutDown());
	}

	@Override
	public void setEventListener(OnEventListener listener) {
		this.onEventListener = listener;
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void closeConnection(){
		if(this.bluetoothClient!=null){
			bluetoothClient.stop();
		}
	}

	public String getMac(){
	 return bluetoothClient.getCurrentMac();
	}
	
	//AA 55 05 81 08 0F 0F 0F 0F 0F 0F 40 00

	
	
}
