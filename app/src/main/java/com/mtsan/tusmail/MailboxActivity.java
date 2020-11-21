package com.mtsan.tusmail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MailboxActivity extends AppCompatActivity
{

    private AppBarConfiguration mAppBarConfiguration;

    private String tusmailServer = "";
    private int tusmailPort = 0;
    private boolean tusmailSSL = true;

    final int SETTINGS_ACTIVITY = 1;
    final int MAILBOX_ACTIVITY = 2;

    private Encryption encryption;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mailbox);

        encryption = Encryption.getInstance(this);
        this.initializeTUSMailPreferences();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mailbox, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void initializeTUSMailPreferences()
    {
        String tusmailServer = this.readPreference(getString(R.string.tusmail_server));
        String tusmailPortString = this.readPreference(getString(R.string.tusmail_port));
        String tusmailSSLString = this.readPreference(getString(R.string.tusmail_ssl));

        if (tusmailServer != null)
        {
            this.setTusmailServer(tusmailServer);
        }
        else
        {
            this.setTusmailServer(getString(R.string.tusmail_default_server));
        }

        if (tusmailPortString != null)
        {
            int port;
            try
            {
                port = Integer.parseInt(tusmailPortString);
                this.setTusmailPort(port);
            }
            catch (Exception e)
            {
                this.setTusmailPort(Integer.parseInt(getString(R.string.tusmail_default_port)));
            }
        }
        else
        {
            this.setTusmailPort(Integer.parseInt(getString(R.string.tusmail_default_port)));
        }

        if (tusmailSSLString != null)
        {
            boolean SSL;
            try
            {
                SSL = Boolean.parseBoolean(tusmailSSLString);
                this.setTusmailSSL(SSL);
            }
            catch (Exception e)
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
        final SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPref.getString(preferenceKey, null);
    }

    private void writePreference(String preferenceKey, String data)
    {
        final SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(preferenceKey, data);
        editor.apply();
    }

    public String getTusmailServer()
    {
        return tusmailServer;
    }

    public int getTusmailPort()
    {
        return tusmailPort;
    }

    public boolean getTusmailSSL()
    {
        return tusmailSSL;
    }

    public void setTusmailServer(String tusmailServer)
    {
        this.tusmailServer = tusmailServer;
    }

    public void setTusmailPort(int tusmailPort)
    {
        this.tusmailPort = tusmailPort;
    }

    public void setTusmailSSL(boolean tusmailSSL)
    {
        this.tusmailSSL = tusmailSSL;
    }

    public void openExtraSettings(View v)
    {
        Intent intent = new Intent(this, TUSMailSettingsActivity.class);
        startActivityForResult(intent, SETTINGS_ACTIVITY);
    }

    public void openPrivacyPolicy(View v)
    {
        Intent intent = new Intent(this, PrivacyPolicy.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == this.SETTINGS_ACTIVITY)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                this.initializeTUSMailPreferences();
            }
        }
    }
}