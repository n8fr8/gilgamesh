package info.guardianproject.gilga.radio;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLEController {

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private Context mContext;
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public boolean init (Context context)
	{
		mContext = context;
		
		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		   // Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
		    return false;
		}
		
		final BluetoothManager bluetoothManager =
		        (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		
		
		
		return true;
	}
	
	public void startDiscovery ()
	{
        mBluetoothAdapter.startLeScan(mLeScanCallback);

	}
	
	public void stopDiscovery ()
	{
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}
	
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
	        new BluetoothAdapter.LeScanCallback() {
	    @Override
	    public void onLeScan(final BluetoothDevice device, int rssi,
	            byte[] scanRecord) {
	    	
	    	mBluetoothGatt = device.connectGatt(mContext, true, mGattCallback);

	            //   mLeDeviceListAdapter.addDevice(device);
	           //    mLeDeviceListAdapter.notifyDataSetChanged();
	           
	   }
	};
	
	// Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
            	
            	mBluetoothGatt.discoverServices();
            	
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
               

            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            	
            } else {
               
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	
            	// For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                }
            }
        }
     
    };


}
