/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.inspector.setup;

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

import com.iflytek.cyber.inspector.BuildConfig;
import com.iflytek.cyber.inspector.LauncherActivity;
import com.iflytek.cyber.inspector.R;

public class WelcomeFragment extends Fragment {

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
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final EditText clientId = view.findViewById(R.id.client_id);
        clientId.setText(pref.getString("client_id", BuildConfig.CLIENT_ID));

        view.findViewById(R.id.next).setOnClickListener(v -> {
            pref.edit()
                    .putString("client_id", clientId.getText().toString())
                    .commit();

            activity.updateClientId();
            activity.navigateTo(new PairFragment());
        });
    }

}
