package info.guardianproject.gilga.model;

import info.guardianproject.gilga.R;
import info.guardianproject.gilga.service.GilgaService;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
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
	    
	    View viewStatus = v.findViewById(R.id.statusbox);
	    TextView txtFrom = (TextView) v.findViewById(R.id.from);
	    TextView txtBody = (TextView) v.findViewById(R.id.body);
	    TextView txtTime = (TextView) v.findViewById(R.id.time);
	    CheckBox cbFav = (CheckBox)v.findViewById(R.id.cbfav);
	    
	    cbFav.setChecked(status.faved);
	    
	    if (status instanceof DirectMessage)
	    {
	    	DirectMessage dm = (DirectMessage)status;

	    	viewStatus.setBackgroundResource(R.color.holo_orange_light);
	    	
	    	if (dm.to != null)
	    	{
		    	String to = dm.to;
		    	
		    	if (to.length() > 6)
			    {
			    	 to = GilgaService.mapToNickname(to);	 
			    }
			    
		    	to = mContext.getString(R.string._pm_to_) + '@' + to;
		    	
		    	if (dm.delivered)
		    		to+=" \u2713";
		    	
		    	txtFrom.setText(to);
		    	
	    	}
	    	else if (dm.from != null)
	    	{
	    		String from = dm.from;
		    	
		    	if (from.length() > 6)
			    {
			    	 from = GilgaService.mapToNickname(from);	 
			    }
			    
		    	from = mContext.getString(R.string._pm_from_) + '@' + from;
		    	
		    	if (dm.delivered)
		    		from+=" \u2713";
		    	
		    	txtFrom.setText(from);

	    	}

	    }
	    else
	    {
	    	if (status.type == Status.TYPE_ALERT)
	    		viewStatus.setBackgroundResource(R.color.holo_red_light);
	    	else if (status.active)
	    		viewStatus.setBackgroundResource(R.color.holo_green_light);
	    	else
		    	viewStatus.setBackgroundResource(R.color.statusboxdefault);

		    String from = status.from;
		    
		    if (status.from.length() > 6)
		    {
		    	 from = GilgaService.mapToNickname(status.from);	 
		    }
		    
		    if (status.trusted)
		    {
		    	from += '*';
		    }
		    
		    String fromText = '@' + from;
		    
		    if (status.reach > 0)
		    {
		    	
		    	if (status.active)
		    	{
		    		fromText += " | " + mContext.getString(R.string.reaching) + ' ' + status.reach + "...";
		    		fromText += "\u2600";
		    	}
		    	else
		    		fromText += " | " + mContext.getString(R.string.reached) + ' ' + status.reach;
		    	
		    }
		    
	    	txtFrom.setText(fromText);
	    }
	    
	    txtBody.setText(status.body);
	    
	    String timeStatus = null;
	    	
	    long timeAgo = (new java.util.Date().getTime() - status.ts)/1000;
	    if (timeAgo < 60)
	    	timeStatus = (timeAgo + mContext.getString(R.string._seconds_ago));
	    else if (timeAgo < 3600)
	    {
	    	timeAgo = timeAgo / 60;
	    	timeStatus = (timeAgo +  mContext.getString(R.string._minutes_ago));
	    }
	    else
	    {
	    	timeAgo = timeAgo / 3600;
	    	timeStatus = (timeAgo +  mContext.getString(R.string._hours_ago));
	    }
	    
	    txtTime.setText(timeStatus);
	    
	    return v;
	};
}