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

import info.guardianproject.gilga.model.DirectMessage;
import info.guardianproject.gilga.model.Status;
import info.guardianproject.gilga.model.StatusAdapter;
import info.guardianproject.gilga.service.GilgaService;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
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
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private ImageButton mSendButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the window layout
        setContentView(R.layout.main);
        
        // Get local Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.please_enable_bluetooth_to_use_this_app, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
       

        // If BT is not on and discoverable, request to make it so -
        // setupChat() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	 startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            
        } 
        else
        {
            setStatus(getString(R.string.broadcast_mode_public_) 
            		+ " | " + getString(R.string.you_are_)
            		+ GilgaService.mapToNickname(bluetoothAdapter.getAddress()));
            
            Intent intent = new Intent(this, GilgaService.class);
            intent.putExtra("listen", true);
            startService(intent);


        }
        setupChat();
        
    }
    
   

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
    }

    private void setupChat() {
//        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(StatusAdapter.getInstance(this));
        mConversationView.setOnItemLongClickListener(new OnItemLongClickListener ()
        {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					final int position, long arg3) {
				
				PopupMenu popupMenu = new PopupMenu(GilgaMeshActivity.this, view);
				popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						
						Status status = (Status)StatusAdapter.getInstance(GilgaMeshActivity.this).getItem(position);

						switch (item.getItemId()) {

						case R.id.item_reshare:

							if (!(status instanceof DirectMessage)) //DM's can't be reshared
								reshareStatus(status);
							return true;
						case R.id.item_copy:
							

							ClipboardManager clipboard = (ClipboardManager)
					        getSystemService(Context.CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("simple text",status.body);
							clipboard.setPrimaryClip(clip);

							
							return true;
						case R.id.item_direct_message:
							
							String dm = "dm " + status.from + " ";
					    	mOutEditText.setText(dm);
					    	mOutEditText.setSelection(dm.length());

							return true;
						default:
							return false;
						}

					}
			
				});
				
				popupMenu.inflate(R.menu.popup_menu);
				popupMenu.show();
				
				
				return false;
			}
        	
        });
        
        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (ImageButton) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                
                updateStatus(message);
            }
        });
    }

    @Override
    public synchronized void onPause() {
        super.onPause();

    }
   

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                updateStatus(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };
    
    
    private void updateStatus (String message)
    {
    	//add message to queue for service to process TODO

        Intent intent = new Intent(this, GilgaService.class);
        intent.putExtra("status", message);
        startService(intent);

        if (!message.matches("^(d |dm ).*$")) //if not a direct message
        {
        	Status statusMe = new Status();
            statusMe.from = getString(R.string.me_);
            statusMe.ts = new java.util.Date().getTime();
            statusMe.trusted = false;
            statusMe.body = message;
            
        	StatusAdapter.getInstance(GilgaMeshActivity.this).add(statusMe);
        
        }
        
        mOutEditText.setText("");
    }

    private void reshareStatus (Status status)
    {
    	String from = status.from;
    	if (from.length() > 6)
    		from = GilgaService.mapToNickname(from);
    	
		String msgRT = "RT @" + from + ' ' + status.body; 
		
		Intent intent = new Intent(this, GilgaService.class);
        intent.putExtra("status", msgRT);
        startService(intent);

        Status statusMe = new Status();
        statusMe.from = getString(R.string.me_);
        statusMe.ts = new java.util.Date().getTime();
        statusMe.trusted = status.trusted;
        statusMe.body = msgRT;
        StatusAdapter.getInstance(GilgaMeshActivity.this).add(statusMe);
	        
			
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

        		 setStatus(getString(R.string.broadcast_mode_public_) 
                 		+ " | " + getString(R.string.you_are_)
                 		+ GilgaService.mapToNickname(bluetoothAdapter.getAddress()));
        		 
                 startService(new Intent(this, GilgaService.class));

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
     //   case R.id.secure_connect_scan:private final IntentFilter intentFilter = new IntentFilter();

            // Launch the DeviceListActivity to see devices and do scan
       //     serverIntent = new Intent(this, DeviceListActivity.class);
         //   startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
           // return true;
        /**
        case R.id.connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
            **/
        case R.id.share_app:
        	shareAPKFile();
        	break;
        }
        return false;
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
