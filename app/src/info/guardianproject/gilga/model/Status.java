package info.guardianproject.gilga.model;

import android.content.Intent;

public class Status {

	public String from;
	public String body;
	public long ts;
	public boolean trusted = false;
	public boolean active = false; //is currently broadcasting
	public int reach; //number of devices in area when sent, and/or potential retweets
	
	public boolean faved = false;
	
	public int type = TYPE_GENERAL;
	
	public final static int TYPE_GENERAL = 0;
	public final static int TYPE_RETWEET = 1;
	public final static int TYPE_REPEAT = 2;
	public final static int TYPE_ALERT = 3;
	
}
