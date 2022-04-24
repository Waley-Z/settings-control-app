package com.example.settingscontrolapp;

import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import android.os.Handler;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSettingsWritePermission();
        setContentView(R.layout.activity_main);

        // components
        Slider brightnessSlider = findViewById(R.id.brightnessSlider);
        Slider volumeSlider = findViewById(R.id.volumeSlider);
        Switch nightSwitch = findViewById(R.id.nightSwitch);
        Switch grayscaleSwitch = findViewById(R.id.grayscaleSwitch);
        Slider animationSlider = findViewById(R.id.animationSlider);

        // brightness
        brightnessSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                Log.d("PRINT", "" + slider.getValue());
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                Log.d("PRINT", "" + slider.getValue());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.System.canWrite(getApplicationContext())) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            modifyBrightness(MainActivity.this, Math.round(slider.getValue() / 100 * 180));
                            toast("System brightness: " + getScreenBrightness(MainActivity.this));
                        }, 0); // delay 5 seconds
                    } else {
                        toast("You have declined to grant permission.");
                    }
                }
            }
        });

        // volume
        volumeSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                Log.d("PRINT", "" + slider.getValue());
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                Log.d("PRINT", "" + slider.getValue());
                modifyVolume(slider.getValue() / 100);
                toast("System media volume: " + getVolume());
            }
        });

        // night mode
        nightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UiModeManager ui = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
            if (isChecked) {
                ui.setNightMode(UiModeManager.MODE_NIGHT_YES);
                toast("Night mode enabled.");
            } else {
                ui.setNightMode(UiModeManager.MODE_NIGHT_NO);
                toast("Night mode disabled.");
            }
        });

        // grayscale
        grayscaleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UiModeManager ui = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
            if (isChecked) {
                Settings.Secure.putString(this.getContentResolver(), "accessibility_display_daltonizer_enabled", "1");
                Settings.Secure.putString(this.getContentResolver(), "accessibility_display_daltonizer", "0");
                toast("Grayscale mode enabled.");
            } else {
                Settings.Secure.putString(this.getContentResolver(), "accessibility_display_daltonizer_enabled", "0");
                Settings.Secure.putString(this.getContentResolver(), "accessibility_display_daltonizer", "-1");
                toast("Grayscale mode disabled.");
            }
        });

        // animation scale
        animationSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                Log.d("PRINT", "" + slider.getValue());
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                Log.d("PRINT", "" + slider.getValue());
                Settings.Global.putString(MainActivity.this.getContentResolver(), "window_animation_scale", String.valueOf(slider.getValue()));
                Settings.Global.putString(MainActivity.this.getContentResolver(), "transition_animation_scale", String.valueOf(slider.getValue()));
                toast("System animation scale: " +
                        Settings.Global.getString(MainActivity.this.getContentResolver(), "window_animation_scale"));
            }
        });
    }

    // create Toast message
    private void toast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    // set screen mode to manual to prevent automatic change
    private void setScreenManualMode(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int mode = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static final int REQUEST_CODE_WRITE_SETTINGS = 1000;

    // ask permission to write system settings
    private void getSettingsWritePermission() {
        // Settings.System.canWrite(MainActivity.this)
        // Checks whether permission to change system settings is granted
        if (!Settings.System.canWrite(MainActivity.this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Permission Request");
            builder.setMessage("Please grant permission to change system settings.");

            // user click no
            builder.setNegativeButton(android.R.string.no, (dialog, which) -> toast("Permission denied."));

            ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
//                                    if (result.getResultCode() == Activity.RESULT_OK) {
//                                        // There are no request codes
//                                        Intent data = result.getData();
                    });

            // user click yes
            builder.setPositiveButton(android.R.string.yes,
                    (dialog, which) -> {
                        // Open settings
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + getPackageName()));
                        activityResultLauncher.launch(intent);
                    });
            builder.setCancelable(false);
            builder.show();
        }
    }

    // get current screen brightness
    private int getScreenBrightness(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        int defVal = 125;
        return Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, defVal);
    }

    // change brightness
    private void modifyBrightness(Context context, int brightnessValue) {
        // Change screen settings to manual mode
        setScreenManualMode(context);

        ContentResolver contentResolver = context.getContentResolver();
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
    }

    // get current volume
    private int getVolume() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    // change volume
    private void modifyVolume(float volumePercentage) {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (audio.isVolumeFixed()) {
            toast("System volume is fixed.");
        } else {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(volumePercentage * streamMaxVolume), AudioManager.FLAG_SHOW_UI);
        }
    }
}