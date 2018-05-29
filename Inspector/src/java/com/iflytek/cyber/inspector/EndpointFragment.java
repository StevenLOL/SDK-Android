package com.iflytek.cyber.inspector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;

public class EndpointFragment extends Fragment {

    private LauncherActivity activity;

    private SharedPreferences pref;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (LauncherActivity) context;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_endpoint, container, false);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final EditText url = view.findViewById(R.id.url);
        url.setText(pref.getString("custom_endpoint", ""));

        final RadioGroup endpoint = view.findViewById(R.id.endpoint);
        endpoint.check(pref.getBoolean("prod", true) ? R.id.prod : R.id.local);

        view.findViewById(R.id.ok).setOnClickListener(v -> {
            pref.edit()
                    .putBoolean("prod", endpoint.getCheckedRadioButtonId() == R.id.prod)
                    .putString("custom_endpoint", url.getText().toString())
                    .commit();

            activity.initMainFragment();
        });
    }

}
