package com.mtsan.tusmail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

public class TUSMailSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private String tusmailServer = "";
        private int tusmailPort = 0;
        private boolean tusmailSSL = true;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            this.initializeTUSMailPreferences();

            EditTextPreference tusmailServerPreference = (EditTextPreference) getPreferenceManager().findPreference("tusmailServer");
            tusmailServerPreference.setText(this.getTusmailServer());
            tusmailServerPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final SharedPreferences sharedPref = preference.getContext().getSharedPreferences(
                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.tusmail_server), newValue.toString());
                    editor.apply();
                    return true;
                }
            });

            ListPreference tusmailPortPreference = (ListPreference) getPreferenceManager().findPreference("tusmailPort");
            tusmailPortPreference.setValue(String.valueOf(this.getTusmailPort()));
            tusmailPortPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final SharedPreferences sharedPref = preference.getContext().getSharedPreferences(
                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.tusmail_port), newValue.toString());
                    editor.apply();
                    return true;
                }
            });

            SwitchPreference tusmailSSLPreference = (SwitchPreference) getPreferenceManager().findPreference("tusmailSSL");
            tusmailSSLPreference.setChecked(this.getTusmailSSL());
            tusmailSSLPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final SharedPreferences sharedPref = preference.getContext().getSharedPreferences(
                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.tusmail_ssl), newValue.toString());
                    editor.apply();
                    return true;
                }
            });

        }

        private void initializeTUSMailPreferences()
        {
            String tusmailServer = this.readPreference(getString(R.string.tusmail_server));
            String tusmailPortString = this.readPreference(getString(R.string.tusmail_port));
            String tusmailSSLString = this.readPreference(getString(R.string.tusmail_ssl));

            if(tusmailServer != null)
            {
                this.setTusmailServer(tusmailServer);
            }
            else
            {
                this.setTusmailServer(getString(R.string.tusmail_default_server));
            }

            if(tusmailPortString != null)
            {
                int port;
                try {
                    port = Integer.parseInt(tusmailPortString);
                    this.setTusmailPort(port);
                }
                catch(Exception e)
                {
                    this.setTusmailPort(Integer.parseInt(getString(R.string.tusmail_default_port)));
                }
            }
            else
            {
                this.setTusmailPort(Integer.parseInt(getString(R.string.tusmail_default_port)));
            }

            if(tusmailSSLString != null)
            {
                boolean SSL;
                try {
                    SSL = Boolean.parseBoolean(tusmailSSLString);
                    this.setTusmailSSL(SSL);
                }
                catch(Exception e)
                {
                    this.setTusmailSSL(Boolean.parseBoolean(getString(R.string.tusmail_default_ssl)));
                }
            }
            else
            {
                this.setTusmailSSL(Boolean.parseBoolean(getString(R.string.tusmail_default_ssl)));
            }
        }

        private String readPreference(String preferenceKey)
        {
            final SharedPreferences sharedPref = this.getActivity().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            return sharedPref.getString(preferenceKey, null);
        }

        private void writePreference(String preferenceKey, String data)
        {
            final SharedPreferences sharedPref = this.getActivity().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(preferenceKey, data);
            editor.apply();
        }

        public String getTusmailServer() {
            return tusmailServer;
        }

        public int getTusmailPort() {
            return tusmailPort;
        }

        public boolean getTusmailSSL() {
            return tusmailSSL;
        }

        public void setTusmailServer(String tusmailServer) {
            this.tusmailServer = tusmailServer;
        }

        public void setTusmailPort(int tusmailPort) {
            this.tusmailPort = tusmailPort;
        }

        public void setTusmailSSL(boolean tusmailSSL) {
            this.tusmailSSL = tusmailSSL;
        }
    }
}