package info.guardianproject.gilga.service;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

public class WifiController {

		private final static String TAG = "GilgaWifi";
			
	   //for WifiP2P mode
	   WifiP2pManager mWifiManager = null;
	   Channel mWifiChannel = null;
	   boolean mWifiEnabled = true;
	   
	   private String mLocalAddressHeader = "";
	   
	   private GilgaService mService;
	   
	   public void init (GilgaService service)
	   {
		   mService = service;

	        mWifiManager = (WifiP2pManager) mService.getSystemService(Context.WIFI_P2P_SERVICE);
	        
	        mWifiChannel = mWifiManager.initialize(mService, mService.getMainLooper(), new ChannelListener()
	        {

				@Override
				public void onChannelDisconnected() {
					Log.d(GilgaService.TAG,"wifi p2p disconnected");
				}
	        	
	        });
	        
	        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
	        mWifiManager.addServiceRequest(mWifiChannel,
	                serviceRequest,
	                new ActionListener() {
	                    @Override
	                    public void onSuccess() {
	                        // Success!
	    	            	Log.d(TAG,"SUCCESS: added service request wifi name service");

	                    }

	                    @Override
	                    public void onFailure(int code) {
	                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	                    	Log.d(TAG,"FAILURED: added service request wifi name service: " + code);
	                    }
	                });
	        
	   }
	   
	   public void stopWifi ()
	   {
		   mWifiManager.stopPeerDiscovery(mWifiChannel, new WifiP2pManager.ActionListener() {

	             @Override
	             public void onSuccess() {
	             	Log.d(TAG,"success on stop discover");
	             	
	             }

	             @Override
	             public void onFailure(int reasonCode) {
	                 // Code for when the discovery initiation fails goes here.
	                 // Alert the user that something went wrong.
	             	Log.d(TAG,"FAIL on stop discovery: " + reasonCode);
	             }

	    	    });
		   
		   // Add the local service, sending the service info, network channel,
	        // and listener that will be used to indicate success or failure of
	        // the request.
	        mWifiManager.clearLocalServices(mWifiChannel, new ActionListener() {
	            @Override
	            public void onSuccess() {
	                // Command successful! Code isn't necessarily needed here,
	                // Unless you want to update the UI or add logging statements.
	            	Log.d(TAG,"SUCCESS: clear local wifi name service");
	            }

	            @Override
	            public void onFailure(int arg0) {
	                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	            	Log.d(TAG,"FAILURE: clear local wifi name service: err=" + arg0);
	            }
	        });
	        
	        mWifiManager.clearServiceRequests(mWifiChannel,	                
	                new ActionListener() {
	                    @Override
	                    public void onSuccess() {
	                        // Success!
	    	            	Log.d(TAG,"SUCCESS: clear service request wifi name service");

	                    }

	                    @Override
	                    public void onFailure(int code) {
	                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	                    	Log.d(TAG,"FAILURED: clear service request wifi name service: " + code);
	                    }
	                });
		   
		   mWifiManager.cancelConnect(mWifiChannel, new WifiP2pManager.ActionListener() {

	             @Override
	             public void onSuccess() {
	             	Log.d(TAG,"success on cancel connect");
	             	
	             }

	             @Override
	             public void onFailure(int reasonCode) {
	                 // Code for when the discovery initiation fails goes here.
	                 // Alert the user that something went wrong.
	             	Log.d(TAG,"FAIL on cancel connect: " + reasonCode);
	             }

	    	    });
	   }
	   
	   public void setEnabled (boolean enabled)
	   {
		   mWifiEnabled = enabled;
	   }
	   
	   public void setLocalAddressHeader (String localAddressHeader)
	   {
		   mLocalAddressHeader = localAddressHeader;//shares with BT address to avoid dup messages
	   }
	   
	   
	   public void setWifiDeviceName (String newDeviceName)
	    {
	    	try
	        {
	    		ActionListener al = new ActionListener ()
	    		{

					@Override
					public void onFailure(int arg0) {
				//		Log.d(TAG,"could not set wifi device name: " + arg0);
					}

					@Override
					public void onSuccess() {
						
					//	Log.d(TAG,"set new device name!");
						
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
	   
	   public void getNetworkInfo ()
	   {

       	// Connection state changed!  We should probably do something about
           // that.
       	/*
       	   NetworkInfo networkInfo = (NetworkInfo) intent
                      .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

              mWifiManager.requestConnectionInfo(mWifiChannel, new ConnectionInfoListener()
              {

				@Override
				public void onConnectionInfoAvailable(WifiP2pInfo winfo) {
					
					
				}
           	   
              });
*/
       	
	   }
	   
	   public void requestPeers ()
	   {
		// The peer list has changed!  We should probably do something about
           // that.
           mWifiManager.requestPeers(mWifiChannel, peerListListener);
	   }
	   
	   public void startWifiDiscovery ()
	    {
	    	
	    	if (mWifiEnabled)
	    	{
	    	 mWifiManager.discoverPeers(mWifiChannel, new WifiP2pManager.ActionListener() {

	             @Override
	             public void onSuccess() {
	                 // Code for when the discovery initiation is successful goes here.
	                 // No services have actually been discovered yet, so this method
	                 // can often be left blank.  Code for peer discovery goes in the
	                 // onReceive method, detailed below.
	             	Log.d(TAG,"success on wifi discover");
	             	
	             }

	             @Override
	             public void onFailure(int reasonCode) {
	                 // Code for when the discovery initiation fails goes here.
	                 // Alert the user that something went wrong.
	             	Log.d(TAG,"FAIL on wifi discovery: " + reasonCode);
	             }

	    	    });
	    	 
	    	 discoverWifiService();
	    	}
	    }
	    
	    private PeerListListener peerListListener = new PeerListListener() {
	        @Override
	        public void onPeersAvailable(WifiP2pDeviceList peerList) {

	          Collection<WifiP2pDevice> deviceList = peerList.getDeviceList();

	          for (WifiP2pDevice device : deviceList)
	          {
	        	  boolean trusted = false; //not sure how to do this with wifi
	        	  
	         	 if (!GilgaService.mapToNickname(device.deviceAddress).startsWith(mLocalAddressHeader)) //not me
	         		mService.processInboundMessage(device.deviceName,device.deviceAddress,trusted);
	        	  
	        	  
	        	  
	          }
	        }
	    };
	    
	    public void updateWifiStatus(String status) {
	    	
	    	setWifiDeviceName(' ' + status);
	    	
	        //  Create a string map containing information about your service.
	        Map record = new HashMap();
	        record.put("status", status);

	        // Service information.  Pass it an instance name, service type
	        // _protocol._transportlayer , and the map containing
	        // information other devices will want once they connect to this one.
	        WifiP2pDnsSdServiceInfo serviceInfo =
	                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

	        mWifiManager.clearLocalServices(mWifiChannel, new ActionListener() {
	            @Override
	            public void onSuccess() {
	                // Command successful! Code isn't necessarily needed here,
	                // Unless you want to update the UI or add logging statements.
	            	Log.d(TAG,"SUCCESS: clear local wifi name service");
	            }

	            @Override
	            public void onFailure(int arg0) {
	                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	            	Log.d(TAG,"FAILURE: clear local wifi name service: err=" + arg0);
	            }
	        });
	        
	        // Add the local service, sending the service info, network channel,
	        // and listener that will be used to indicate success or failure of
	        // the request.
	        mWifiManager.addLocalService(mWifiChannel, serviceInfo, new ActionListener() {
	            @Override
	            public void onSuccess() {
	                // Command successful! Code isn't necessarily needed here,
	                // Unless you want to update the UI or add logging statements.
	            	Log.d(TAG,"SUCCESS: enabled local wifi name service");
	            }

	            @Override
	            public void onFailure(int arg0) {
	                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	            	Log.d(TAG,"FAILURE: enabled local wifi name service: err=" + arg0);
	            }
	        });
	        
	
	    }
	    
	    private void discoverWifiService() {
	        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
	            @Override
	            /* Callback includes:
	             * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
	             * record: TXT record dta as a map of key/value pairs.
	             * device: The device running the advertised service.
	             */

	            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
	                    
	                    String status = (String)record.get("status");
	                    
	                    Log.d(TAG,"got status from wifi DNS: " + device.deviceAddress + " (" + fullDomain + ") " + status);
	                    mService.processInboundMessage(device.deviceAddress,status,false);
	                }
	            
	            };
	        
	            DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
	                @Override
	                public void onDnsSdServiceAvailable(String instanceName, String registrationType,
	                        WifiP2pDevice resourceType) {
	                	Log.d(TAG,"SD service available!");
	                }
	            };

	            mWifiManager.setDnsSdResponseListeners(mWifiChannel, servListener, txtListener);
	            
	            mWifiManager.discoverServices(mWifiChannel, new ActionListener() {

	                @Override
	                public void onSuccess() {
	                    // Success!
	                	Log.d(TAG, "discover services!");
	                }

	                @Override
	                public void onFailure(int code) {
	                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	                    if (code == WifiP2pManager.P2P_UNSUPPORTED) {
	                        Log.d(TAG, "P2P isn't supported on this device.");
	                        mWifiEnabled = false;
	                    }
	                }
	            });

	    }
}
