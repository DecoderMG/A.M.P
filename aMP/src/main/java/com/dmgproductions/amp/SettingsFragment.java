package com.dmgproductions.amp;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.ViewGroup;

public class SettingsFragment extends PreferenceFragment {
    
   public SettingsFragment()
   {}
    
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
	   super.onCreate(savedInstanceState);
	   addPreferencesFromResource(R.xml.preferences);
   }
   
}