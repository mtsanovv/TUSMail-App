package com.mtsan.tusmail.ui.login;

import android.app.Activity;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.security.ProviderInstaller;
import com.mtsan.tusmail.Encryption;
import com.mtsan.tusmail.PrivacyPolicy;
import com.mtsan.tusmail.R;
import com.mtsan.tusmail.TUSMailSettingsActivity;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.*;

public class LoginActivity extends AppCompatActivity
{

    private LoginViewModel loginViewModel;

    private String tusmailServer = "";
    private int tusmailPort = 0;
    private boolean tusmailSSL = true;

    final int SETTINGS_ACTIVITY = 1;

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar loadingProgressBar;
    private TextView errorMessage;
    private CheckBox checkBox;

    private Encryption encryption;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.loading);
        errorMessage = findViewById(R.id.errorMessage);
        checkBox = findViewById(R.id.checkBox);

        encryption = Encryption.getInstance(this);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>()
        {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState)
            {
                if (loginFormState == null)
                {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null)
                {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null)
                {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    toggleLoading(false);
                }
                return false;
            }
        });

        this.initializeTUSMailPreferences();

        String tusmailMail = readPreference(getString(R.string.tusmail_saved_email));
        String tusmailPassword = readPreference(getString(R.string.tusmail_saved_password));

        if(tusmailMail != null && tusmailPassword != null)
        {
            try
            {
                String decryptedMail = encryption.decrypt(this, tusmailMail);
                String decryptedPassword = encryption.decrypt(this, tusmailPassword);
                usernameEditText.setText(decryptedMail);
                passwordEditText.setText(decryptedPassword);
                checkBox.setChecked(true);

                toggleLoading(true);
                attemptLogin(decryptedMail, decryptedPassword, false);
            }
            catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException | BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }

        loginButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                toggleLoading(true);

                attemptLogin(usernameEditText.getText().toString(), passwordEditText.getText().toString(), true);
            }
        });
    }

    private void attemptLogin(String username, String password, Boolean attemptingFormLogin)
    {
        errorMessage.setVisibility(View.INVISIBLE);
        try
        {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //Attempt POP3 login
        Properties properties = System.getProperties();
        properties.setProperty("mail.pop3.host", this.getTusmailServer() + ".tu-sofia.bg");
        properties.setProperty("mail.pop3.port", String.valueOf(this.getTusmailPort()));
        properties.setProperty("mail.pop3.connectiontimeout", "10000"); //10s socket connect timeout
        properties.setProperty("mail.pop3.timeout", "10000"); //10s socket read timeout
        properties.setProperty("mail.pop3.connectionpooltimeout", "10000"); //10s socket write timeout
        if (this.getTusmailSSL())
        {
            properties.setProperty("mail.pop3.ssl.enable", "true");
        }

        Session session = Session.getDefaultInstance(properties);

        LoginActivity loginActivity = this;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Store store = session.getStore("pop3");
                    store.connect(username, password);
                    Folder inbox = store.getFolder("Inbox");
                    inbox.open(Folder.READ_ONLY);
                    inbox.close(true);
                    store.close();

                    if(attemptingFormLogin && checkBox.isChecked())
                    {
                        try
                        {
                            String encryptedUsername = encryption.encrypt(loginActivity, username);
                            String encryptedPassword = encryption.encrypt(loginActivity, password);
                            writePreference(getString(R.string.tusmail_saved_email), encryptedUsername);
                            writePreference(getString(R.string.tusmail_saved_password), encryptedPassword);
                        }
                        catch(NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException | BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    //all the code below is nice for later

                    /*
                    Message[] messages = inbox.getMessages();

                    if (messages.length == 0) System.out.println("No messages found.");

                    for (int i = 0; i < messages.length; i++)
                    {

                        System.out.println("Message " + (i + 1));
                        System.out.println("From : " + messages[i].getFrom()[0]);
                        System.out.println("Subject : " + messages[i].getSubject());
                        System.out.println("Sent Date : " + messages[i].getSentDate());
                        System.out.println();
                    }
                     */
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            String message = e.getMessage();
                            System.out.println(message);
                            switch (message)
                            {
                                case "[AUTH] Authentication failed.":
                                    errorMessage.setText(getString(R.string.authFailed));
                                    writePreference(getString(R.string.tusmail_saved_email), null);
                                    writePreference(getString(R.string.tusmail_saved_password), null);
                                    break;
                                case "Connect failed":
                                    errorMessage.setText(getString(R.string.connectFailed));
                                    break;
                                case "[AUTH] Plaintext authentication disallowed on non-secure (SSL/TLS) connections.":
                                    errorMessage.setText(getString(R.string.sslError));
                                    break;
                                default:
                                    if (message.toLowerCase().contains("timeout") || message.toLowerCase().contains("timed out"))
                                    {
                                        errorMessage.setText(getString(R.string.connectTimeout));
                                    }
                                    else
                                    {
                                        errorMessage.setText(getString(R.string.errorOccurred));
                                    }
                                    break;
                            }
                            errorMessage.setVisibility(View.VISIBLE);
                            toggleLoading(false);
                        }
                    });
                }
            }
        }).start();
    }

    private void toggleLoading(boolean toggle)
    {
        if (toggle)
        {
            loadingProgressBar.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
        }
        else
        {
            loadingProgressBar.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
        }
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

    private void updateUiWithUser(LoggedInUserView model)
    {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString)
    {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}