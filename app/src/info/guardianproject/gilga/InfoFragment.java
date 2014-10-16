package info.guardianproject.gilga;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class InfoFragment extends Fragment
{

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		 View rootView = inflater.inflate(
	                R.layout.info, container, false);
		 
		return rootView;
	}

}
