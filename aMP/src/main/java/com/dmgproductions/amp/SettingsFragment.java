package com.dmgproductions.amp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

   public SettingsFragment()
   {}

   @Override
   public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
   {
	   setPreferencesFromResource(R.xml.preferences, rootKey);
   }

}
