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

import info.guardianproject.gilga.service.GilgaService;

import java.io.File;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class GilgaMeshActivity extends Activity {
    // Debugging
    private static final String TAG = "GILGA";
    private static final boolean D = true;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;

    private boolean mRepeaterMode = false;

    private String mLocalAddress = null;
    
    private Handler mHandler = new Handler(); //for posting delayed events
    
    private StatusListFragment mStatusList;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the window layout
        setContentView(R.layout.main);
        
        checkBluetooth();

        setupTabbedBar();
        
    }
    
    public BluetoothAdapter checkBluetooth ()
    {
        // Get local Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.please_enable_bluetooth_to_use_this_app, Toast.LENGTH_LONG).show();
            finish();
            return null;
        }

        // If BT is not on and discoverable, request to make it so -
        // setupChat() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	 startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            
        } 
        else
        {
        	mLocalAddress = GilgaService.mapToNickname(bluetoothAdapter.getAddress());

        	if (bluetoothAdapter.getScanMode() ==
        	          BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {        		
        	
                setStatus(getString(R.string.broadcast_mode_public_) 
                		+ " @"	+ mLocalAddress);
        	}
        	else    
        		setStatus(getString(R.string.listen_mode));
        	
       
            Intent intent = new Intent(this, GilgaService.class);
            intent.putExtra("listen", true);
            startService(intent);


        }
     
        return bluetoothAdapter;
    }
    
    private void setupTabbedBar ()
    {
    	   final ActionBar actionBar = getActionBar();

    	    // Specify that tabs should be displayed in the action bar.
    	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    	    // Create a tab listener that is called when the user changes tabs.
    	    ActionBar.TabListener tabListener = new ActionBar.TabListener() {
    	        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
    	          
    	        	switch (tab.getPosition())
    	        	{
    	        		case 0:
    	        			((ListView) findViewById(R.id.statusList)).setAdapter(GilgaApp.mStatusAdapter);
    	        			
    	        			break;
    	        		case 1:
    	        			((ListView) findViewById(R.id.statusList)).setAdapter(GilgaApp.mFavAdapter);
    	        			
    	        			break;
    	        		default:
    	        	}
    	        			
    	        }

    	        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    	            // hide the given tab
    	        }

    	        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    	            // probably ignore this event
    	        }
    	    };

    	    // Add 3 tabs, specifying the tab's text and TabListener
    	    
	        actionBar.addTab(
	                actionBar.newTab()
	                        //.setIcon(android.R.drawable.ic_dialog_alert)
	                		.setText("Status")
	                        .setTabListener(tabListener));
    	    
	        actionBar.addTab(
	                actionBar.newTab()
	                        //.setIcon(android.R.drawable.ic_dialog_alert)
	                		.setText("Favs")
	                        .setTabListener(tabListener));
	        
	        actionBar.addTab(
	                actionBar.newTab()
	                        //.setIcon(android.R.drawable.ic_dialog_alert)
	                		.setText("Mesh")
	                        .setTabListener(tabListener));
	        
	        actionBar.addTab(
	                actionBar.newTab()
	                        //.setIcon(android.R.drawable.ic_dialog_alert)
	                		.setText("Info")
	                        .setTabListener(tabListener));
    	    
    
    }
    
   

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
    }

  
    @Override
    public synchronized void onPause() {
        super.onPause();

    }
   

    
    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		
    	super.onConfigurationChanged(newConfig);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
       
        case REQUEST_ENABLE_BT:
        	if (resultCode == Activity.RESULT_OK)
        	{
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                mLocalAddress = GilgaService.mapToNickname(bluetoothAdapter.getAddress());

            	if (bluetoothAdapter.getScanMode() ==
            	          BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {        		
            	
                    setStatus(getString(R.string.broadcast_mode_public_) 
                    		+ " @"	+ mLocalAddress);
            	}
            	else    
            		setStatus(getString(R.string.listen_mode));
            	
           
                Intent intent = new Intent(this, GilgaService.class);
                intent.putExtra("listen", true);
                startService(intent);

        	}
        	
        	break;
        /*
        case REQUEST_CONNECT_DEVICE:Br
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {

                if (mChatService != null) {
                    // Only if the state is STATE_NONE, do we know that we haven't started already
                    if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                      // Start the Bluetooth chat services
                      mChatService.start();
                    }
                    
                }

                // Get the device MAC address
                String address = data.getExtras()
                    .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                connectDevice(address);
            }
            break;
        */
        }
    }

  

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.share_app:
        	shareAPKFile();
        	break;
        case R.id.shutdown_app:
        	shutdown();
        	break;
        case R.id.toggle_visibility:
        	toggleVisibility(false);
        	break;
        case R.id.toggle_repeater:
        	toggleRepeater();        	
        	break;
        }
        return false;
    }
        
    public void toggleVisibility (boolean forceVisible)
    {
        final BluetoothAdapter bluetoothAdapter = checkBluetooth();

    	if (bluetoothAdapter.getScanMode() !=
    	          BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
    	        	
    	            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
    	            startActivity(discoverableIntent);
    	            
    	        }
    	else if (!forceVisible)
    	{
    		  Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
	            startActivity(discoverableIntent);
    	}
    	
    	mHandler.postDelayed(new Runnable ()
    	{
    		public void run ()
    		{
		    	if (bluetoothAdapter.getScanMode() ==
		  	          BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {        		
		  	
		          setStatus(getString(R.string.broadcast_mode_public_) 
		          		+ " @"	+ mLocalAddress);
			  	}
			  	else    
			  		setStatus(getString(R.string.listen_mode));
    		}
    	},5000);
    }
    
    private void toggleRepeater ()
    {
    	mRepeaterMode = !mRepeaterMode;

        Intent intent = new Intent(this, GilgaService.class);
        intent.putExtra("repeat", mRepeaterMode);
        startService(intent);
    }
    
    private void shutdown ()
    {

        Intent intent = new Intent(this, GilgaService.class);
        stopService(intent);
        finish();
    }

    private void shareAPKFile ()
    {
    	PackageManager pm = getPackageManager();

    //	String thisPkgId = pm.getPackageInfo(getPackageName(), 0).packageName;
    			
        String uri = null;
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            if(!((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1))
                if(!((app.flags & ApplicationInfo.FLAG_SYSTEM) == 1)){
                    uri=app.sourceDir;
                      if(uri.contains(getPackageName()))
                      break;
                }
        }

        Intent intent = new Intent();  
        intent.setAction(Intent.ACTION_SEND);  
     //   intent.setPackage("com.android.bluetooth"); //let's not limit it to Bluetooth... who knows how you might want to share a file!
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(uri)));
        startActivity(intent);
    }
    
  

}
