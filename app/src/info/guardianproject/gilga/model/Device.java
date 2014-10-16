package info.guardianproject.gilga.model;

import info.guardianproject.gilga.service.GilgaService;
import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;

public class Device {

	public final static int TYPE_BLUETOOTH_CLASSIC = 0;
	public final static int TYPE_BLUETOOTH_LE = 1;
	public final static int TYPE_WIFI_DIRECT = 2;

	public int mType;
	public String mName;
	public String mAddress;
	public boolean mTrusted;
	public String mSignalInfo;
	
	public Object mInstance;

	
	public Device (BluetoothDevice bDevice)
	{
		mAddress = bDevice.getAddress();
		mName = GilgaService.mapToNickname(mAddress);
		mType = TYPE_BLUETOOTH_CLASSIC;
		mTrusted = bDevice.getBondState() == BluetoothDevice.BOND_BONDED;
		
		mInstance = bDevice;
	}
	
	public Device (WifiP2pDevice wDevice)
	{
		mAddress = wDevice.deviceAddress;
		mName = GilgaService.mapToNickname(wDevice.deviceAddress);
		mType = TYPE_WIFI_DIRECT;
		mTrusted = false;
		
		mInstance = wDevice;
	}
}
