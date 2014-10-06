package info.guardianproject.gilga;

import android.content.Intent;

public class Status {

	String from;
	String body;
	long ts;
	boolean trusted = false;
	int type;
	
	public static final int TYPE_PUBLIC = 1;
	public static final int TYPE_DIRECT = 2;
	
	public static Status inflate (Intent intent)
	{
		Status msg = new Status();
		msg.from = intent.getStringExtra("from");
		msg.body = intent.getStringExtra("body");
		msg.ts = intent.getLongExtra("ts",0);
		msg.trusted = intent.getBooleanExtra("trusted", false);
		msg.type = intent.getIntExtra("type", Status.TYPE_PUBLIC);
		
		return msg;
	}
}
