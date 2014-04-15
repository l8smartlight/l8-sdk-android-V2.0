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



```
