package info.guardianproject.gilga.radio;

import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BluetoothClassicController {

	private final static String TAG = "BTUTIL";
	
	public static void pairDevice(BluetoothDevice device) {
		try {
		    
		    Log.d(TAG, "Start Pairing...");

		    Method m = device.getClass()
		        .getMethod("createBond", (Class[]) null);
		    m.invoke(device, (Object[]) null);
		    
		    Log.d(TAG, "Pairing finished.");
		} catch (Exception e) {
		    Log.e(TAG, e.getMessage());
		}
		}

	public static void unpairDevice(BluetoothDevice device) {
		try {
		    Method m = device.getClass()
		        .getMethod("removeBond", (Class[]) null);
		    m.invoke(device, (Object[]) null);
		} catch (Exception e) {
		    Log.e(TAG, e.getMessage());
		}
	}
}
