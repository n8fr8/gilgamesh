package info.guardianproject.gilga;

import android.content.Intent;

public class Status {

	String from;
	String body;
	long ts;
	boolean trusted = false;
	int type;
	
	public static Status inflate (Intent intent)
	{
		Status msg = new Status();
		msg.from = intent.getStringExtra("from");
		msg.body = intent.getStringExtra("body");
		msg.ts = intent.getLongExtra("ts",0);
		msg.trusted = intent.getBooleanExtra("trusted", false);
		
		return msg;
	}
}
