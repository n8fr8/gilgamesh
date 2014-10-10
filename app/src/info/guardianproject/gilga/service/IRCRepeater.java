package info.guardianproject.gilga.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.util.Log;

public class IRCRepeater implements Runnable {

	String mServer;
	String mNick;
	String mLogin;
	String mChannel;
	BufferedWriter mWriter;
	BufferedReader mReader;
	Socket mSocket;
	Thread mThread;
	
    public IRCRepeater(String name, String channel) {

        // The server to connect to and our details.
        mServer = "irc.freenode.net";
        mNick = name;
        mLogin = name;
        mChannel = channel;
        
        mThread = new Thread(this);
        mThread.start();
    }
    
    public boolean isConnected ()
    {
    	return mSocket != null && mSocket.isConnected();
    }
    
    public void shutdown ()
    {
    	try
    	{
    		mSocket.close();
    	}
    	catch (IOException e)
    	{
    		Log.e("ImService","error closing socket",e);
    	}
    	
    }
    
    public void sendMessage (String msg) throws IOException
    {
    	mWriter.write("PRIVMSG " + mChannel + " :" + msg + "\r\n");
    	mWriter.flush( );
    }
    
    public void run ()
    {
    	try
    	{
	        // Connect directly to the IRC server.
    		mSocket = new Socket(mServer, 6667);
	        mWriter = new BufferedWriter(
	                new OutputStreamWriter(mSocket.getOutputStream( )));
	        mReader = new BufferedReader(
	                new InputStreamReader(mSocket.getInputStream( )));
	       
	        // Log on to the server.
	        mWriter.write("NICK " + mNick + "\r\n");
	        mWriter.write("USER " + mLogin + " 8 * : Java IRC Hacks Bot\r\n");
	        mWriter.flush( );
	        
	        // Read lines from the server until it tells us we have connected.
	        String line = null;
	        while ((line = mReader.readLine( )) != null) {
	            if (line.indexOf("004") >= 0) {
	                // We are now logged in.
	                break;
	            }
	            else if (line.indexOf("433") >= 0) {
	                System.out.println("Nickname is already in use.");
	                return;
	            }
	        }
	        
	        // Join the channel.
	        mWriter.write("JOIN " + mChannel + "\r\n");
	        mWriter.flush( );
	        
	        // Keep reading lines from the server.
	        while ((line = mReader.readLine( )) != null) {
	            if (line.startsWith("PING ")) {
	                // We must respond to PINGs to avoid being disconnected.
	            	mWriter.write("PONG " + line.substring(5) + "\r\n");
	            	mWriter.write("PRIVMSG " + mChannel + " :I got pinged!\r\n");
	            	mWriter.flush( );
	            }            
	        }
	        
	        if (!mSocket.isClosed())
	        	mSocket.close();
    	}
    	catch (IOException ioe)
    	{
    		Log.e("IRCRepeater","error connecting",ioe);
    	}
    }

}
