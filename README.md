![L8smartlight](http://corcheaymedia.com/l8/wp-content/plugins/wp-l8-styles/images/logo.png)
L8 smartlight Android SDK V2.0
=========================

**News:** 
We are proud to anounce the new l8 sdk version for Android platform, thanks you for your interest and patience. We are in continuous evolution thanks to all!.

If you have done something with the previous version SDK V1.0, migrate your code to this new version is very easy to you. Installation and configuration steps are very similar. 

**Note:** 
Now a javadoc is available in /doc. In this documentation all functions and new features of V2.0 are shown to you.  

**Note:**
Thre is an easy way to create apps for your L8 without this SDK, you can use Exposed Api for more details:  [Exposed-Android-API](https://github.com/l8devteam/Exposed-Android-API)

## 1. Installation:

1.Clone the projects. 
    
2.Import in eclipse and check as a library.
    
3.Create a new projet and select l8-sdk-android as a referenced library. Maybe is also a good idea to get into the classpath.
    
    - Insert in AndroidManifest.xml the activity.

        <activity
            android:name="com.l8smartlight.sdk.android.bluetooth.DeviceListActivity"
            android:label="@string/txt_select_device"
            android:theme="@android:style/Theme.Dialog" 
        />
 
4.Insert in strings.xml: string txt_select_device.

       <string name="txt_select_device">Select device</string>

	   

## 2. Quick start

You have to instantiate AndroidL8Manager and implement AndroidL8ManagerListener. Now you have more control about connections and devices. In this new version L8 Simultor is optional and you don´t have to use it.

**Example of use:**

```java
public class MainActivity extends Activity implements AndroidL8ManagerListener{

	private AndroidL8Manager manager = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		manager = new AndroidL8Manager(this); 
		manager.registerListener(this); //You must register listener if you wanto to recive events for manager.
		                                //N listeners can be registered.
					        
		manager.init(this);             //Initializes AndroidL8Manager and starts connetions or scan.
	}


	//bluetooth not available in the system, it is a very rare situation but could be happen.
	@Override
	public void bluetoothNotAvailable() {
		Toast.makeText(this,"BT Not Available" , 0).show();
	}
	
	//Bluetooth not enabled. 
	@Override
	public void bluetoothNotEnabled() {
		Toast.makeText(this,"BT not enabled" , 0).show();
	}

	//AndroidL8Manager doesn´t have any device registered and you should start a new scan.
	@Override
	public void noDevicesRegistered() {
		manager.scan(this);
	}

	//AndroidL8Manager calls this method when a device is connected.
	@Override
	public void deviceConnected(L8 l8) {
		try {
			Toast.makeText(this,"Device connected "+l8.getConnectionURL() , 0).show();
		} catch (L8Exception e) {
			e.printStackTrace();
		}
	}
	
	//AndroidL8Manager calls this method when a device has been disconnected.
	@Override
	public void deviceDisconnected(L8 l8) {
		try {
			Toast.makeText(this,"Device disconnected" , 0).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//As the previous version it is requered if you want to use GUI helpers and automatic reponses.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		manager.onActivityResult(requestCode, resultCode, data);
	}
	
	//AndroidL8Manager calls this method when you have lost all connections or when can not connect to any device
	//after an init call.
	@Override
	public void noDevicesConnected() {
		Toast.makeText(this,"No Devices Connected", 0).show();
		manager.requestSimulator();
	}
	
	//AndroidL8Manager calls this method when it has finished all initial connections and at least has one device connected 
	@Override
	public void allConnectionsDone() {
		Toast.makeText(this,"All connections done", 0).show();
	}
	
	//Optionally you can request a similator for test purpose. It is an asynchronous operation.
	@Override
	public void simulatorRequested(L8 l8, boolean newSimulator) {
		try
		{
		if(l8==null){
			Toast.makeText(this,"Simulator null", 0).show();
		}else if(newSimulator){
			Toast.makeText(this,"New Simulator "+l8.getConnectionURL(), 0).show();
		}else{
			Toast.makeText(this,"Old Simulator "+l8.getConnectionURL(), 0).show();
		}
		}catch(Exception e){
			Toast.makeText(this,"Error simulator", 0).show();
		}
	}
	

	public void pressButton(View view){
		try {
			//set x y Led
			manager.setLED(0, 0, Color.BLUE);
			//set Super led
			manager.setSuperLED(Color.RED);
		} catch (L8Exception e) {
			e.printStackTrace();
		}
	}


```

## 3. How it works

Now when you want to set a led or put a new image in a L8 device, you can do all kind of operations through AndroidL8Manager.

**Example of use:**

```java
   
   public void pressButton(View view){
		try {
			//set x y Led
			manager.setLED(0, 0, Color.BLUE);
			//set Super led
			manager.setSuperLED(Color.RED);
		} catch (L8Exception e) {
			e.printStackTrace();
		}
	}
   
```
 A detailed documentation about all functions can be consulted in javadoc section.

**Note:**
All operations through manager will affect to all connected devices, by default all are selected. You can operate with a specific L8 in this way: [Exposed API](https://github.com/l8devteam/Exposed-Android-API)

**Example of use:**

```java
   
   public void pressButton(View view){
		try {
			//set x y Led
			manager.getConnectedDeviceList().get(0).setLED(0, 0, Color.BLUE);
			//set Super led
		        manager.getConnectedDeviceList().get(0).setSuperLED(Color.RED);
		} catch (L8Exception e) {
			e.printStackTrace();
		}
	}
   
```

Other way:

Suppose you have two L8 connected.

**Example of use:**

```java
   
   public void pressButton(View view){
		try {
			manager.getConnectedDeviceList().get(0).setSelected(false);
			//set x y Led
			manager.setLED(0, 0, Color.BLUE);
			//set Super led
			manager.setSuperLED(Color.RED);
		} catch (L8Exception e) {
			e.printStackTrace();
		}
	}
	
```
This this case only the second L8 will change its leds.


## 4. What is new in V2.0

1- Now you can control multiple L8s.

2- You have more control over connections.

3- Simultor is an optional feature.

4- More information is provided.


