package info.guardianproject.gilga.service;

import info.guardianproject.gilga.GilgaApp;
import info.guardianproject.gilga.GilgaMeshActivity;
import info.guardianproject.gilga.R;
import info.guardianproject.gilga.model.Device;
import info.guardianproject.gilga.model.DirectMessage;
import info.guardianproject.gilga.model.Status;
import info.guardianproject.gilga.radio.WifiController;
import info.guardianproject.gilga.uplink.IRCUplink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class GilgaService extends Service {

	public final static String TAG = "GilgaService";

	public final static String ACTION_NEW_MESSAGE = "action_new_message";
    public final static String MATCH_DIRECT_MESSAGE = "(?i)^(d |dm |pm ).*$";
    
 // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private final static int BLUETOOTH_DISCOVERY_RETRY_TIMEOUT = 12000;
    
   //Local Device Address
   private String mLocalShortBluetoothAddress = "";
   private String mLocalAddressHeader = "";
   
   private WifiController mWifiController;
   public static Hashtable<String,Device> mDeviceMap = new Hashtable<String,Device>();
   
    boolean mRepeaterMode = false; //by default RT trusted messages
    boolean mRepeatToIRC = false; //need to add more options here    
    private IRCUplink mIRCRepeater = null;
    private final static String DEFAULT_IRC_CHANNEL = "#gilgamesh";
    
    
    private Status mLastStatus = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    
    private static Hashtable<String,Status> mMessageLog = null; //uses hash to ensure we don't display dup messages
    
    private ArrayList<DirectMessage> mQueuedDirectMessage = new ArrayList<DirectMessage>();
    private DirectMessageSession mDirectChatSession;
    
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		init();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	
		if (intent != null)
		{
			if (intent.hasExtra("status"))
			{
				String status = intent.getStringExtra("status");
				
				if (status.matches(MATCH_DIRECT_MESSAGE))
				{
					sendDirectMessage (status);
				}
				else
				{
					if (mLastStatus != null)
						mLastStatus.active = false;
					
		        	mLastStatus = new Status();
		        	mLastStatus.from = getString(R.string.me_);
		        	mLastStatus.ts = new java.util.Date().getTime();
		        	mLastStatus.trusted = false;
		        	mLastStatus.body = status;
		        	mLastStatus.reach = mDeviceMap.size();
		            mLastStatus.active = true;
		            
		            if (intent.hasExtra("type"))
		            	mLastStatus.type = intent.getIntExtra("type", Status.TYPE_GENERAL);
		            	
			        GilgaApp.mStatusAdapter.add(mLastStatus);
			        
					updateStatus (status);
				}
				
			}
			
			if (intent.hasExtra("repeat"))
			{
				mRepeaterMode = intent.getBooleanExtra("repeat", false);
				
				if (mRepeatToIRC)
				{
					if (mRepeaterMode)
						mIRCRepeater = new IRCUplink(mLocalShortBluetoothAddress,DEFAULT_IRC_CHANNEL);
					else if (mIRCRepeater != null)
						mIRCRepeater.shutdown();
				}
			}
		}
		
		startListening();
		
		startForegroundNotify();
		
	    return (START_STICKY);
	}


	@Override
    public void onDestroy() {
        super.onDestroy();
        
        stopForeground(true);
        // Stop the Bluetooth chat services
        if (mDirectChatSession != null) mDirectChatSession.stop();
        
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering())
        {
        	mBluetoothAdapter.cancelDiscovery();      
        	mBluetoothAdapter = null;
        }
        
        mWifiController.stopWifi();
        
        this.unregisterReceiver(mReceiver);

    }

	private void startForegroundNotify ()
	{
		
		String message = getString(R.string.app_name) + getString(R.string._is_running);
		
		if (mRepeaterMode)
			message += " | " + getString(R.string.repeater_enabled);
			
		Notification.Builder builder =
    		    new Notification.Builder(this)
    		    .setSmallIcon(R.drawable.ic_notify)
    		    .setContentTitle(getString(R.string.app_name))
    		    .setContentText(message);
    	
		if (mRepeaterMode)
			builder.setTicker(getString(R.string.repeater_enabled));		
    	        
    	Intent resultIntent = new Intent(this, GilgaMeshActivity.class);
    	
    	// Because clicking the notification opens a new ("special") activity, there's
    	// no need to create an artificial back stack.
    	PendingIntent resultPendingIntent =
    	    PendingIntent.getActivity(
    	    this,
    	    0,
    	    resultIntent,
    	    PendingIntent.FLAG_UPDATE_CURRENT
    	);
    	
    	builder.setContentIntent(resultPendingIntent);

    	// Sets an ID for the notification
    	int mNotificationId = 002;
    	
    	startForeground(mNotificationId,builder.getNotification());
	}

	public static Hashtable<String,Status> getMessageLog ()
	{
		return mMessageLog;
	}

	private void init ()
	{
		
		
		mMessageLog = new Hashtable<String,Status>();
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mLocalShortBluetoothAddress = mapToNickname(mBluetoothAdapter.getAddress());
        mLocalAddressHeader = mLocalShortBluetoothAddress.substring(0,5);
       // mChatService = new BluetoothChatService(this, mHandler);

        mWifiController = new WifiController();
        mWifiController.init(this);
        
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
        
        registerReceiver(mReceiver, filter);
        
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        mHandler.postDelayed(mBluetoothChecker, BLUETOOTH_DISCOVERY_RETRY_TIMEOUT);
	}

	private Runnable mBluetoothChecker = new Runnable ()
	{
		public void run ()
    	{
    		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
    		{     
    			try
    			{
	            	if (!mBluetoothAdapter.isDiscovering())
	            		mBluetoothAdapter.startDiscovery();
            		          
	            }
            	catch (Exception e){}
    		}
    		
    		mHandler.postDelayed(mBluetoothChecker, BLUETOOTH_DISCOVERY_RETRY_TIMEOUT);
    	}
	};
    
    public boolean processInboundMessage (String name, String address, boolean trusted)
    {
    	String messageBuffer = name;
    	
    	StringTokenizer st = new StringTokenizer(messageBuffer,"\n");
    	
    	boolean isNewDevice = false;
    	
    	while (st.hasMoreTokens())
    	{
    	
    		String message = st.nextToken();
    		
        	if (message.startsWith("#")||
        			message.startsWith("!")||
        			message.startsWith("@")||
        			message.startsWith(".")||
        			message.startsWith(" "))
        	{
        		
        		message = message.trim();
        		
        		Status status = new Status();
        		status.from = address;
        		status.body = message;
        		status.trusted = trusted;
        		status.ts = new java.util.Date().getTime();
        		
            	if (isNewMessage(status)) //have we seen this message before
            	{
            		isNewDevice = true;
            		
            		if (message.startsWith("!"))
            		{
            			status.type = Status.TYPE_ALERT;
            			String alertMsg = '@' + mapToNickname(status.from) + ": " + status.body;
    	                sendNotitication(getString(R.string.alert),alertMsg);
            		}
            		
            		GilgaApp.mStatusAdapter.add(status);
            		
            		if (mRepeaterMode             				
            						&& (!message.contains('@' + mLocalAddressHeader))
            				) //don't RT my own tweet
            		{
            			String rtMessage = "RPT @" + mapToNickname(status.from) + ": " + status.body;
            			updateStatus(rtMessage); //retweet!

            			try
            			{
            				mIRCRepeater.sendMessage(rtMessage);
            			}
            			catch (IOException e)
            			{
            				Log.e(TAG,"error repeating to IRC",e);
            			}
            			
            			Status statusMe = new Status();
                        statusMe.from = getString(R.string.me_);
                        statusMe.ts = status.ts;
                        statusMe.trusted = trusted;
                        statusMe.body = rtMessage;
                        statusMe.type = Status.TYPE_REPEAT;
                        
                		GilgaApp.mStatusAdapter.add(statusMe);

            		}
            		
            	}
            
        	}
    	}
    	
    	return isNewDevice;
    }
    
    
    
    private boolean isNewMessage (Status msg)
    {
    	
    	if (msg.body.indexOf(':')!=-1)
    	{
    	    
    		String messageBody = msg.body.substring(msg.body.lastIndexOf(':')+1).trim();
    		String hash = MD5(messageBody);
    		
    		if (mMessageLog.containsKey(hash))
    			return false;
    		else
    		{
    			mMessageLog.put(hash, msg);
    			return true;
    		}
    	}
    	else
    	{
    		String hash = MD5(msg.body);
    		if (mMessageLog.containsKey(hash))
    			return false;
    		else
    		{
    			mMessageLog.put(hash, msg);
    			return true;
    		}
    	}
    	
    	
    }
    
   
    
    private void startBroadcasting() {
      //  if(D) Log.d(TAG, "ensure discoverable");
       if (mBluetoothAdapter.getScanMode() !=
          BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        	
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(discoverableIntent);
            
        }
       
       if (!mBluetoothAdapter.isDiscovering())
    	   mBluetoothAdapter.startDiscovery();

       if (mDirectChatSession == null)
       {
    	   mDirectChatSession = new DirectMessageSession(this, mHandler);
    	   mDirectChatSession.start();
       }
       else {
           // Only if the state is STATE_NONE, do we know that we haven't started already
           if (mDirectChatSession.getState() == DirectMessageSession.STATE_NONE) {
             // Start the Bluetooth chat services
        	   mDirectChatSession.start();
           }
           
       }
        
    }
    
    private void startListening ()
    {
        
    	if (!mBluetoothAdapter.isDiscovering())
    		mBluetoothAdapter.startDiscovery();
        
    	mWifiController.startWifiDiscovery ();
       
    }
    
    private void sendDirectMessage (String message)
    {
    	
    	StringTokenizer st = new StringTokenizer(message," ");
    	String cmd = st.nextToken();
    	String address = st.nextToken();
    	
    	if (address.equals(mLocalShortBluetoothAddress)
    			|| address.equals(mBluetoothAdapter.getAddress()))
    			{
    		//can't send DM's to yourself
    		Toast.makeText(this, R.string.you_can_t_send_private_messages_to_yourself, Toast.LENGTH_SHORT).show();
    		return;
    			}
    	
    	try
    	{
	    	final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	    	final boolean isSecure = device.getBondState()==BluetoothDevice.BOND_BONDED;
	    	
	    	StringBuffer dMessage = new StringBuffer();
	    	
	    	while (st.hasMoreTokens())
	    		dMessage.append(st.nextToken()).append(" ");
	    	
	    	DirectMessage dm = new DirectMessage();
	    	dm.to = address;
	    	dm.body = dMessage.toString().trim();
	    	dm.ts = new java.util.Date().getTime();
	    	dm.delivered = false;
	
	    	mQueuedDirectMessage.add(dm);
	    	
	    	dm.trusted = isSecure;
	    	GilgaApp.mStatusAdapter.add(dm);
	    	
	    	if (mDirectChatSession == null)
	    	{
	    	 mDirectChatSession = new DirectMessageSession(this, mHandler);
	  	   	 mDirectChatSession.start();
	    	}
	    	else
	    	{
	    		mDirectChatSession.disconnect();
	    	}
	    	
	    	mHandler.postAtTime(new Runnable ()
	    	{
	    		public void run ()
	    		{
	    	    	mDirectChatSession.connect(device, isSecure);
	    		}
	    	}, 2000);
    	}
    	catch (IllegalArgumentException iae)
    	{
    		Toast.makeText(this, getString(R.string.error_sending_message_) + iae.getLocalizedMessage(), Toast.LENGTH_LONG).show();
    	}
    	
    }
    
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void updateStatus(String message) {

        // Check that there's actually something to send
        if (message.length() > 0) {
           
        	mOutStringBuffer.append(' ' + message + '\n');

        	if (mOutStringBuffer.toString().getBytes().length > 248)
        	{
        		  mOutStringBuffer.setLength(0);
        		  mOutStringBuffer.append(' ' + message + '\n');
        	}
        	
        	mBluetoothAdapter.setName(mOutStringBuffer.toString());
        	
        	mWifiController.updateWifiStatus(message);
    		startBroadcasting() ;
            
            
        }
    }
    
    public static String mapToNickname (String hexAddressIn)
    {
    	String shortAddress = new String(hexAddressIn);
    	if (shortAddress.length() > 6)
    	{
	    	//remove : and get last 6 characters
    		shortAddress = shortAddress.replace(":", "");
    		shortAddress = shortAddress.substring(shortAddress.length()-6,shortAddress.length());	   
    	}
    	
    	return shortAddress.toUpperCase();
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
    
 // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                //if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case DirectMessageSession.STATE_CONNECTED:
                   
                	//once connected, send message, then wait for response
                	String address = msg.getData().getString("address");
                	ArrayList<DirectMessage> listSent = new ArrayList<DirectMessage>();
                	
                	if (address != null)
                	{
                		synchronized (mQueuedDirectMessage)
                		{
	                		Iterator<DirectMessage> itDm = mQueuedDirectMessage.iterator();
	                		while (itDm.hasNext())
	                		{
	                			DirectMessage dm = itDm.next();
	                			
	                			if (dm.to.equals(address))
	                			{
	                				String dmText = dm.body + '\n';
	                				mDirectChatSession.write(dmText.getBytes());
	                				dm.delivered = true;
	                				GilgaApp.mStatusAdapter.notifyDataSetChanged();
	                				listSent.add(dm);
	                			}
	                		}
                		}
                		
                		
                	}
                	
                	mQueuedDirectMessage.removeAll(listSent);
                	
                    break;
                case DirectMessageSession.STATE_CONNECTING:
                  //  setStatus(R.string.title_connecting);
                    break;
                case DirectMessageSession.STATE_LISTEN:
                case DirectMessageSession.STATE_NONE:
                //    setStatus(getString(R.string.broadcast_mode_public_) + " | " + getString(R.string.you_are_) + mLocalAddress);
                    break;
                }
                break;
            case MESSAGE_WRITE:
            	
            	//we just add it directly, but we should mark as delivered here
            	/**
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                
                Status status = new Status();
                status.from = getString(R.string.me_);
                status.body = writeMessage;
                status.trusted = true;
                status.type = Status.TYPE_DIRECT;
                status.ts = new java.util.Date().getTime();
                **/
            //    GilgaApp.mStatusAdapter.add(status);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                String addr = msg.getData().getString("address");
                
                StringTokenizer st = new StringTokenizer (readMessage,"\n");
                
                while (st.hasMoreTokens())
                {
	                DirectMessage dm = new DirectMessage();
	                dm.from = addr;
	                dm.body = st.nextToken();
	                dm.trusted = true;
	                dm.ts = new java.util.Date().getTime();
	                
	                sendNotitication(getString(R.string._pm_from_) + addr, dm.body);
	                
	                GilgaApp.mStatusAdapter.add(dm);
                }
                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
         //       mConnectedDeviceName = mapToNickname(msg.getData().getString(DEVICE_NAME));
               // Toast.makeText(getApplicationContext(), R.string.connected_to_
                 //              + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
               // Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                 //              Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void sendNotitication (String title, String message)
    {
    	Notification.Builder builder =
    		    new Notification.Builder(this)
    		    .setSmallIcon(R.drawable.ic_notify)
    		    .setContentTitle(title)
    		    .setContentText(message);
    	
    	  //Vibration
        builder.setVibrate(new long[] { 500, 1000, 500 });
        builder.setAutoCancel(true);

     //LED
        builder.setLights(Color.BLUE, 3000, 3000);
        
    	Intent resultIntent = new Intent(this, GilgaMeshActivity.class);
    	
    	// Because clicking the notification opens a new ("special") activity, there's
    	// no need to create an artificial back stack.
    	PendingIntent resultPendingIntent =
    	    PendingIntent.getActivity(
    	    this,
    	    0,
    	    resultIntent,
    	    PendingIntent.FLAG_UPDATE_CURRENT
    	);
    	
    	builder.setContentIntent(resultPendingIntent);

    	// Sets an ID for the notification
    	int mNotificationId = 001;
    	// Gets an instance of the NotificationManager service
    	NotificationManager mNotifyMgr = 
    	        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    	// Builds the notification and issues it.
    	mNotifyMgr.notify(mNotificationId, builder.getNotification());
    }
    
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
                	String address = device.getAddress();
                	
                	boolean isNewStatusOrDevice = processInboundMessage(device.getName(),address,device.getBondState() == BluetoothDevice.BOND_BONDED);
                	
                	if (isNewStatusOrDevice) //this is a gilgamesh device
                	{
                		Device d = new Device(device);
                		mDeviceMap.put(device.getAddress(), d);
                		
                        int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                        d.mSignalInfo = rssi + context.getString(R.string.dbm);
                		
                		//if we have a last status, increase the number of devices reached
                		if (mLastStatus != null)
                			mLastStatus.reach = mDeviceMap.size(); //set to current size
                        
                	}
                	else
                	{
                		Device d = mDeviceMap.get(device.getAddress());
                		if (d != null)
                		{
                            int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                            d.mSignalInfo = rssi + context.getString(R.string.dbm);
                		}
                	}
                	
                	if (mQueuedDirectMessage.size() > 0 && mDirectChatSession != null
                			&& (mDirectChatSession.getState() != DirectMessageSession.STATE_CONNECTED 
                			|| mDirectChatSession.getState() != DirectMessageSession.STATE_CONNECTING))
                	{
                		//try to do resend now if address matches
                		
                		if (address != null)
                    	{
                			synchronized (mQueuedDirectMessage)
                			{
	                    		Iterator<DirectMessage> itDm = mQueuedDirectMessage.iterator();
	                    		while (itDm.hasNext())
	                    		{
	                    			DirectMessage dm = itDm.next();
	                    			
	                    			if (dm.to.equals(address))
	                    			{
	                    		    	boolean isSecure = device.getBondState()==BluetoothDevice.BOND_BONDED;
	                    		    	mDirectChatSession.connect(device, isSecure);
	                    		    	break;
	                    			}
	                    		}
                			}
                    	}
                	}
                	
                }
                
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                
            	mHandler.postDelayed(new Runnable ()
            	{
            		public void run ()
            		{
            			if (mBluetoothAdapter != null)
            				mBluetoothAdapter.startDiscovery();
            		}
            	}, BLUETOOTH_DISCOVERY_RETRY_TIMEOUT);
            }
            else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                
                	mWifiController.setEnabled(true);
                } 
                
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            	mWifiController.requestPeers();
            	
                

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            	mWifiController.getNetworkInfo ();
            	
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                
            	WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            	
            	mDeviceMap.put(device.deviceAddress, new Device(device));
        		
        		//if we have a last status, increase the number of devices reached
        		if (mLastStatus != null)
        			mLastStatus.reach = mDeviceMap.size(); //set to current size

            	 boolean trusted = false; //not sure how to do this with wifi
            	 
            	 if (!mapToNickname(device.deviceAddress).startsWith(mLocalAddressHeader)) //not me
           	  		processInboundMessage(device.deviceName,device.deviceAddress,trusted);
            }
        }
    };

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
  
}
