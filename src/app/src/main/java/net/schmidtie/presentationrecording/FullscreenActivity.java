package net.schmidtie.presentationrecording;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends Activity {
    private static final String TAG = "FullscreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Settings settings = SettingsActivity.ReadSettings(this);
        Log.d(TAG, settings.toString());

        setContentView(R.layout.activity_fullscreen);

        if (settings.Autostart_HdmiIn){
            startActivity(new Intent(this, RecordActivity.class));
        }
    }

    public void startSettings(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void startHdmi(View v) {
        startActivity(new Intent(this, RecordActivity.class));
    }
}
