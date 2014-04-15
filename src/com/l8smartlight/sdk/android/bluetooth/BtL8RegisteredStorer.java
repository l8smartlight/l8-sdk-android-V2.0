package com.l8smartlight.sdk.android.bluetooth;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;


public class BtL8RegisteredStorer {

	private static final String REGISTERED_BT_FILE_NAME = "BtL8RegisteredStorer.data";
	
	public static void saveRegisteredL8(Context context, List<String> contacts) {
		try {
			FileOutputStream file = context.openFileOutput(REGISTERED_BT_FILE_NAME, Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(file);
			out.writeObject(contacts);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e("BtL8RegisteredStorer", "Error openning file: "+e);
		} catch (IOException e) {
			Log.e("BtL8RegisteredStorer", "Error "+e);
		} 
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<String> retriveRegisteredL8(Context context){
		ArrayList<String> result = null;
		try {
			FileInputStream file = context.openFileInput(REGISTERED_BT_FILE_NAME);
			ObjectInputStream in = new ObjectInputStream(file);
			result = (ArrayList<String>)in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			Log.e("BtL8RegisteredStorer", "Error openning file: "+e);
		} catch (IOException e) {
			Log.e("BtL8RegisteredStorer", "Error "+e);
		}catch (ClassNotFoundException e) {
			Log.e("BtL8RegisteredStorer", "Error "+e);
		}
		return result;
	}
	
}
