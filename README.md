![L8smartlight](http://corcheaymedia.com/l8/wp-content/plugins/wp-l8-styles/images/logo.png)
L8 smartlight Android SDK V2.0
=========================

**News:** 
We are proud to anounce the new l8 sdk version for Android platform, thanks you for your interest and patience. We are in continuous evolution thanks to all!.

If you have done something with the previous version SDK V1.0, migrate your code to this new version is very easy to you. Installation and configuration steps are very similar. 

**Note:** 
Now a javadoc is available in /doc. In this documentation all functions and new features of V2.0 are shown to you.  

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

You have to instantiate AndroidL8Manager and implement AndroidL8ManagerListener. Now you have more control about connections and devices.

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
		manager.init(this);
	}



	@Override
	public void bluetoothNotAvailable() {
		Toast.makeText(this,"BT Not Available" , 0).show();
	}

	@Override
	public void bluetoothNotEnabled() {
		Toast.makeText(this,"BT not enabled" , 0).show();
	}

	@Override
	public void noDevicesRegistered() {
		manager.scan(this);
	}

	@Override
	public void deviceConnected(L8 l8) {
		try {
			Toast.makeText(this,"Device connected "+l8.getConnectionURL() , 0).show();
		} catch (L8Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void deviceDisconnected(L8 l8) {
		try {
			Toast.makeText(this,"Device disconnected" , 0).show();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		manager.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void noDevicesConnected() {
		Toast.makeText(this,"No Devices Connected", 0).show();
		manager.requestSimulator();
	}

	@Override
	public void allConnectionsDone() {
		Toast.makeText(this,"All connections done", 0).show();
	}

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
	


```
