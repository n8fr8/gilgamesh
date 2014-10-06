package info.guardianproject.gilga;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class StatusAdapter extends BaseAdapter
{
	private static StatusAdapter mInstance;
	private ArrayList<Status> mArrayStatus;
	private Context mContext;
	
	private StatusAdapter (Context context)
	{
		mArrayStatus = new ArrayList<Status>();
		mContext = context;
	}
	
	public synchronized static StatusAdapter getInstance (Context context)
	{
		if (mInstance == null)
			mInstance = new StatusAdapter(context);
		
		return mInstance;
	}
	
	public void add (Status status)
	{
		mArrayStatus.add(status);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return mArrayStatus.size();
	}

	@Override
	public Object getItem(int idx) {
		return mArrayStatus.get(idx);
	}

	@Override
	public long getItemId(int arg0) {
		return (long)arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
	   
	    if (v == null) {
	        // Inflate the layout according to the view type
	        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	       
	        v = inflater.inflate(R.layout.status, parent, false);
	        
	    }
	    
	    Status status = mArrayStatus.get(position);
	    
	    TextView txtFrom = (TextView) v.findViewById(R.id.from);
	    TextView txtBody = (TextView) v.findViewById(R.id.body);
	    TextView txtTime = (TextView) v.findViewById(R.id.time);
	    
	    String from = status.from;
	    
	    if (status.from.length() > 6)
	    {
	    	 from = GilgaService.mapToNickname(status.from);	 
	    }
	    
	    if (status.trusted)
	    	from += '*';
	    
	    String fromText = '@' + from;
	    		
	    if (status.type == Status.TYPE_DIRECT)
	    {
	    	fromText = ('@' + from + " (DIRECT MESSAGE)");
	    	v.setBackgroundResource(R.color.holo_orange_light);
	    }
	    else
	    {
	    	v.setBackgroundResource(android.R.color.transparent);
	    }
	    
	    txtFrom.setText(fromText);
	    
	    txtBody.setText(status.body);
	    
	    long timeAgo = (new java.util.Date().getTime() - status.ts)/1000;
	    if (timeAgo < 60)
	    	txtTime.setText(timeAgo + " seconds ago");
	    else if (timeAgo < 3600)
	    {
	    	timeAgo = timeAgo / 60;
	    	txtTime.setText(timeAgo + " minutes ago");
	    }
	    else
	    {
	    	timeAgo = timeAgo / 3600;
	    	txtTime.setText(timeAgo + " hours ago");
	    }
	    
	    return v;
	};
}