package info.guardianproject.gilga;

import info.guardianproject.gilga.model.StatusAdapter;

import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GilgaApp extends Application {


    public static StatusAdapter mStatusAdapter;
    public static StatusAdapter mFavAdapter;
    
    
	@Override
	public void onCreate() {
		super.onCreate();
		
	}

    
}
