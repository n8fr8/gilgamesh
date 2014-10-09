package info.guardianproject.gilga.model;

import android.content.Intent;

public class Status {

	public String from;
	public String body;
	public long ts;
	public boolean trusted = false;
	public int type;
	
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
