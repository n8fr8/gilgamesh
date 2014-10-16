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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
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
public class StatusListFragment extends Fragment {
    // Debugging
    private static final String TAG = "GILGA";
    private static final boolean D = true;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private ImageButton mSendButton;

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.status_list, container, false);
      
        setupView(rootView);
     
        return rootView;
    }
    
    public void switchAdapter(StatusAdapter sa)
    {
    	 mConversationView.setAdapter(sa);
    }

    private void setupView(final View rootView) {


        // Initialize the array adapter for the conversation thread
        mConversationView = (ListView) rootView.findViewById(R.id.statusList);
        mConversationView.setAdapter(GilgaApp.mStatusAdapter);
        mConversationView.setOnItemLongClickListener(new OnItemLongClickListener ()
        {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					final int position, long arg3) {
				
				PopupMenu popupMenu = new PopupMenu(getActivity(), view);
				popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						
						Status status = (Status)GilgaApp.mStatusAdapter.getItem(position);

						switch (item.getItemId()) {

						case R.id.item_reshare:

							if (!(status instanceof DirectMessage)) //DM's can't be reshared
								reshareStatus(status);
							return true;
						case R.id.item_copy:
							

							ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("simple text",status.body);
							clipboard.setPrimaryClip(clip);

							
							return true;
						case R.id.item_reply:
							
							String reply = "@" + GilgaService.mapToNickname(status.from) + " ";
					    	mOutEditText.setText(reply);
					    	mOutEditText.setSelection(reply.length());

							return true;
						case R.id.item_direct_message:
							
							String dm = "pm " + status.from + " ";
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
				
				
				return true;
			}
        	
        });
        
        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) rootView.findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (ImageButton) rootView.findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) rootView.findViewById(R.id.edit_text_out);
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
    
    
    private void updateStatus (String status)
    {

    	((GilgaMeshActivity)getActivity()).toggleVisibility(true);
    	
        Intent intent = new Intent(getActivity(), GilgaService.class);
        intent.putExtra("status", status);
        getActivity().startService(intent);

        mOutEditText.setText("");
        
        
    }

    private void reshareStatus (Status status)
    {
    	((GilgaMeshActivity)getActivity()).toggleVisibility(true);
    	
    	String from = status.from;
    	if (from.length() > 6)
    		from = GilgaService.mapToNickname(from);
    	
		String msgRT = "RT @" + from + ' ' + status.body; 
		
		Intent intent = new Intent(getActivity(), GilgaService.class);
        intent.putExtra("status", msgRT);
        intent.putExtra("type", Status.TYPE_RETWEET);
        getActivity().startService(intent);

    }  
    
}
