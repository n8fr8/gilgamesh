/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.gilga;

import info.guardianproject.gilga.model.Device;
import info.guardianproject.gilga.radio.BluetoothClassicController;
import info.guardianproject.gilga.service.GilgaService;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListFragment extends Fragment {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private Handler mHandler = new Handler(); //for posting delayed events

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.device_list, container, false);
        
        // Initialize the button to perform device discovery
        Button scanButton = (Button) rootView.findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View arg0) {
				refreshDevices();
				
			}
        	
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);
        
        refreshDevices ();
        
        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) rootView.findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) rootView.findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
    	rootView.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

              
        /*
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
        	rootView.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }*/
        

        return rootView;
    }
    
    private void refreshDevices ()
    {
    	 
    	mPairedDevicesArrayAdapter.clear();
    	mNewDevicesArrayAdapter.clear();
    	
    	
        for (Device device: GilgaService.mDeviceMap.values())
        {
        	if (device.mTrusted)
        		mPairedDevicesArrayAdapter.add(formatItem(device));
        }
        
        for (Device device: GilgaService.mDeviceMap.values())
        {
        	if (!device.mTrusted)
        		mNewDevicesArrayAdapter.add(formatItem(device));
        }

    }
    
    private String formatItem (Device device)
    {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append('@');
    	sb.append(device.mName);
    	sb.append(" (");
    	
    	if (device.mType == Device.TYPE_BLUETOOTH_CLASSIC)
    		sb.append (getString(R.string.bluetooth));
    	else if (device.mType == Device.TYPE_BLUETOOTH_LE)
    		sb.append (getString(R.string.bluetoothle));
    	else if (device.mType == Device.TYPE_WIFI_DIRECT)
    		sb.append (getString(R.string.wifidirect));	
    	
    	if (device.mSignalInfo != null)
    	{
    		sb.append(' ');
    		sb.append(device.mSignalInfo);
    	}
    	
    	sb.append(")");
    	sb.append('\n');
    	sb.append(device.mAddress);
    	
    	return sb.toString();
    }
	@Override
	public void onHiddenChanged(boolean hidden) {
		// TODO Auto-generated method stub
		super.onHiddenChanged(hidden);
		
		if (!hidden)
			refreshDevices();
	}
	

	private void togglePairing (Device device)
	{

		device.mTrusted = !device.mTrusted; //toggle trusted state
		
		if (device.mType == Device.TYPE_BLUETOOTH_CLASSIC)
		{
	        BluetoothDevice bDevice = (BluetoothDevice)device.mInstance;
	        
	    	if (device.mTrusted)
	    	{
	    		BluetoothClassicController.pairDevice(bDevice);
	    		Toast.makeText(getActivity(), R.string.add_device_to_trusted_list_, Toast.LENGTH_LONG).show();
	
	    	}
	    	else
	    	{
	    		BluetoothClassicController.unpairDevice(bDevice);
	    		Toast.makeText(getActivity(), R.string.removing_device_from_trusted_list_, Toast.LENGTH_LONG).show();
	
	    	}
	
		}
		

    	mHandler.postDelayed(new Runnable ()
    	{
    		public void run ()
    		{
    			refreshDevices();
    		}
    	}
    	, 5000);
	}

	// The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            //mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            Device device = GilgaService.mDeviceMap.get(address);
        
            togglePairing(device);
        }
    };

}
