package com.dmgproductions.amp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

public class OnboardingFragment extends Fragment {

    private RelativeLayout localCard, streamingCard;
    private Button continueButton;
    private String selectedMode = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_onboarding, container, false);

        localCard = (RelativeLayout) rootView.findViewById(R.id.local_mode_card);
        streamingCard = (RelativeLayout) rootView.findViewById(R.id.streaming_mode_card);
        continueButton = (Button) rootView.findViewById(R.id.continue_button);

        localCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMode("local");
            }
        });

        streamingCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMode("streaming");
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save onboarding status and transition
                TinyDB db = new TinyDB(getActivity());
                db.putBoolean("onboarding_complete", true);
                db.putString("music_mode", selectedMode);
                
                // Go to Home
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_container, new HomeFragment())
                        .commit();
            }
        });

        return rootView;
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        continueButton.setVisibility(View.VISIBLE);
        
        if (mode.equals("local")) {
            localCard.setBackgroundResource(R.drawable.card_selected_bg);
            streamingCard.setBackgroundResource(R.drawable.pill_toggle_bg);
        } else {
            streamingCard.setBackgroundResource(R.drawable.card_selected_bg);
            localCard.setBackgroundResource(R.drawable.pill_toggle_bg);
        }
    }
}
