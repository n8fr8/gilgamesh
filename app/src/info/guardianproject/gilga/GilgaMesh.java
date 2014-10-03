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

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class GilgaMesh extends Activity {
    // Debugging
    private static final String TAG = "GILGA";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    
    //Local Device Address
    private String mLocalAddress = null;
    
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private Hashtable<String,Date> mMessageLog = null;
    
    private boolean mRepeaterMode = true; //by deafult RT trusted messages
    
    //for WifiP2P mode
    WifiP2pManager mWifiManager = null;
    Channel mWifiChannel = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.please_enable_bluetooth_to_use_this_app, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        mLocalAddress = mapToNickname(mBluetoothAdapter.getAddress());
        
        
        mChatService = new BluetoothChatService(this, mHandler);

        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiChannel = mWifiManager.initialize(this, getMainLooper(), new ChannelListener()
        {

			@Override
			public void onChannelDisconnected() {
				Log.d(TAG,"wifi p2p disconnected");
			}
        	
        });
        
        IntentFilter filter = new IntentFilter();
        // Register for broadcasts when a device is discovered
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        // Register for broadcasts when discovery has finished
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        //  Indicates a change in the Wi-Fi P2P status.
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        this.registerReceiver(mReceiver, filter);

        // If BT is not on and discoverable, request to make it so -
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	 startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            
        } else {
             setupChat();
        }
        
      //  createTabs();
        
    }
    
    private void setWifiDeviceName (String newDeviceName)
    {
    	try
        {
    		ActionListener al = new ActionListener ()
    		{

				@Override
				public void onFailure(int arg0) {
					Log.d(TAG,"could not set wifi device name: " + arg0);
				}

				@Override
				public void onSuccess() {
					
					Log.d(TAG,"set new device name!");
					
				}
    			
    		};
    		
	        Class c;
	        c = Class.forName("android.net.wifi.p2p.WifiP2pManager");
	        Method m = c.getMethod("setDeviceName", new Class[] {Channel.class, String.class, ActionListener.class});
	        Object o = m.invoke(mWifiManager, new Object[]{mWifiChannel,newDeviceName, al});
	        
        }
        catch (Exception e){
        	
        	Log.e(TAG,"error setting wifi name",e);
        }
    }
    
    
    private void createTabs ()
    {
    	 ActionBar actionBar = getActionBar();
         actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
  
         actionBar.setDisplayShowTitleEnabled(true);
  
         /** Creating ANDROID Tab */
         Tab tab = actionBar.newTab()
             .setText("! Broadcast");
         
         //    .setTabListener(new CustomTabListener<AndroidFragment>(this, "android", AndroidFragment.class))
          //   .setIcon(R.drawable.android);
  
         actionBar.addTab(tab);
  
         /** Creating APPLE Tab */
         tab = actionBar.newTab()
             .setText("@ Replies");
 
         actionBar.addTab(tab);
         
         tab = actionBar.newTab()
                 .setText("Direct Messages");
     
             actionBar.addTab(tab);
             
             
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

      
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        mMessageLog = new Hashtable<String,Date>();
        
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mConversationView.setOnItemLongClickListener(new OnItemLongClickListener ()
        {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				String message = mConversationArrayAdapter.getItem(arg2);
				mOutEditText.setText("RT @" + message); 
				
				return false;
			}
        	
        });
        
        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

   
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        startListening();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
        
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering())
        	mBluetoothAdapter.cancelDiscovery();
        
        this.unregisterReceiver(mReceiver);

    }

    private void startBroadcasting() {
        if(D) Log.d(TAG, "ensure discoverable");
       if (mBluetoothAdapter.getScanMode() !=
          BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        	
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            startActivity(discoverableIntent);
            
        }

       if (mChatService != null) {
           // Only if the state is STATE_NONE, do we know that we haven't started already
           if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
             // Start the Bluetooth chat services
             mChatService.start();
           }
           
       }
        
    }
    
    private void startListening ()
    {
        
        if (!mBluetoothAdapter.isDiscovering())
        	mBluetoothAdapter.startDiscovery();
        
        startWifiDiscovery ();
       
    }
    
    private void startWifiDiscovery ()
    {
    	 mWifiManager.discoverPeers(mWifiChannel, new WifiP2pManager.ActionListener() {

             @Override
             public void onSuccess() {
                 // Code for when the discovery initiation is successful goes here.
                 // No services have actually been discovered yet, so this method
                 // can often be left blank.  Code for peer discovery goes in the
                 // onReceive method, detailed below.
             	Log.d(TAG,"success on wifi discover");
             	//startWifiDiscovery();
             }

             @Override
             public void onFailure(int reasonCode) {
                 // Code for when the discovery initiation fails goes here.
                 // Alert the user that something went wrong.
             	Log.d(TAG,"FAIL on wifi discovery: " + reasonCode);
             }

    	    });
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            
            if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
            	mChatService.write(send);
            else
            {
            	
            	mOutStringBuffer.append(' ' + message + '\n');

            	if (mOutStringBuffer.toString().getBytes().length > 248)
            	{
            		  mOutStringBuffer.setLength(0);
            		  mOutStringBuffer.append(' ' + message + '\n');
            	}
            	
            	mBluetoothAdapter.setName(mOutStringBuffer.toString());
            	
            	setWifiDeviceName(' ' + message);
            	
        		mConversationArrayAdapter.add(getString(R.string.me_)+ message);
        		
        		startBroadcasting() ;
            }
            
            		
            // Reset out string buffer to zero and clear the edit text field
          
            mOutEditText.setText("");
            
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName) + ' ' + getString(R.string._private_));
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    setStatus(getString(R.string.broadcast_mode_public_) + " | " + getString(R.string.you_are_) + mLocalAddress);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add(getString(R.string.me_) + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = mapToNickname(msg.getData().getString(DEVICE_NAME));
               // Toast.makeText(getApplicationContext(), R.string.connected_to_
                 //              + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		
    	super.onConfigurationChanged(newConfig);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
       
        case REQUEST_CONNECT_DEVICE:
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
        
        }
    }

    private void connectDevice(String address) {
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        
        //start direct chat, if paied, then enable a secure connection
        mChatService.connect(device, device.getBondState() == BluetoothDevice.BOND_BONDED);
        
        // Attempt to connect to the device);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            
        	if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
        		mChatService.disconnect();
        	
            return true;
        }

        return super.onKeyDown(keyCode, event);
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
     //   case R.id.secure_connect_scan:private final IntentFilter intentFilter = new IntentFilter();

            // Launch the DeviceListActivity to see devices and do scan
       //     serverIntent = new Intent(this, DeviceListActivity.class);
         //   startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
           // return true;
        case R.id.connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.share_app:
        	shareAPKFile();
        	break;
        }
        return false;
    }
    
    private PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

          Collection<WifiP2pDevice> deviceList = peerList.getDeviceList();

          for (WifiP2pDevice device : deviceList)
          {
        	  processInboundMessage(device.deviceName,device.deviceAddress,false);
        	  
          }
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                if (device.getName() != null)
                {
                	processInboundMessage(device.getName(),device.getAddress(),device.getBondState() == BluetoothDevice.BOND_BONDED);
                }
                
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                
                mBluetoothAdapter.startDiscovery();
            }
            else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //	Toast.makeText(GilgaMesh.this, "Wifi P2P enhanced mode activated!", Toast.LENGTH_LONG).show();
                
                } 
                
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                // The peer list has changed!  We should probably do something about
                // that.
                mWifiManager.requestPeers(mWifiChannel, peerListListener);

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // Connection state changed!  We should probably do something about
                // that.

            	Log.d(TAG,"connection changed");
            	
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                
            	WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            }
        }
    };
    
    private void processInboundMessage (String name, String address, boolean trusted)
    {
    	String messageBuffer = name;
    	String from = address;
    	
    	StringTokenizer st = new StringTokenizer(messageBuffer,"\n");
    	
    	while (st.hasMoreTokens())
    	{
    	
    		String message = st.nextToken();
    		
        	if (message.startsWith("#")||message.startsWith("!")||message.startsWith("@")||message.startsWith(" "))
        	{
        		message = message.trim();
        	
            	if (isNewMessage(message)) //have we seen this message before
            	{	
            		from = mapToNickname (address);
            		
            		if (trusted)
            			from += '*';
            		
            		mConversationArrayAdapter.add(from + ": " + message);
            		
            		if (trusted && mRepeaterMode && (!message.contains('@' + mLocalAddress))) //don't RT my own tweet
            		{
            			String rtMessage = "RT @" + from + ": " + message;
            			sendMessage(rtMessage); //retweet!
            			
            		}
            	}
        	}
    	}
    }
    
    private boolean isNewMessage (String message)
    {
    	String messageBody = message;
    	
    	if (messageBody.indexOf(':')!=-1)
    	{
    		messageBody = messageBody.substring(messageBody.lastIndexOf(':')+1).trim();
    		String hash = MD5(messageBody);
    		if (mMessageLog.containsKey(hash))
    			return false;
    		else
    		{
    			mMessageLog.put(hash, new Date());
    			return true;
    		}
    	}
    	else
    	{
    		String hash = MD5(messageBody);
    		if (mMessageLog.containsKey(hash))
    			return false;
    		else
    		{
    			mMessageLog.put(hash, new Date());
    			return true;
    		}
    	}
    	
    	
    }
    
    public static String mapToNickname (String hexAddress)
    {
    	//remove : and get last 6 characters
    	hexAddress = hexAddress.replace(":", "");
    	hexAddress = hexAddress.substring(hexAddress.length()-7,hexAddress.length()-1);
    	return hexAddress.toUpperCase();
    }
    
    public String MD5(String md5) {
    	   try {
    	        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
    	        byte[] array = md.digest(md5.getBytes());
    	        StringBuffer sb = new StringBuffer();
    	        for (int i = 0; i < array.length; ++i) {
    	          sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
    	       }
    	        return sb.toString();
    	    } catch (java.security.NoSuchAlgorithmException e) {
    	    }
    	    return null;
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
        intent.setPackage("com.android.bluetooth");
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(uri)));
        startActivity(intent);
    }
}
