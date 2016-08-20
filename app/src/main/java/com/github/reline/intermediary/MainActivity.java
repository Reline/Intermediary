package com.github.reline.intermediary;

import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    private Menu menu;

    // text to speech
    @BindView(R.id.currentLang) TextView currLang;
    @BindView(R.id.input) EditText input;
    private TextToSpeech tts;
    @BindView(R.id.speak) Button speakButton;
    @OnClick(R.id.speak) void onSpeakButtonClicked() {
        tts.speak(input.getText(), TextToSpeech.QUEUE_FLUSH, null, null);
    }
    @OnClick(R.id.stop) void onStopButtonClicked() {
        if(tts.isSpeaking()) {
            tts.stop();
        }
    }
    private Locale userLocale;
    private Locale[] availableLanguages;

    // accelerometer
    private Vibrator vibrator;
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(MainActivity.this.getString(R.string.loading));
        progressDialog.show();

        initAccelerometer(); // set up accelerometer

        availableLanguages = Locale.getAvailableLocales(); // get all of the available locales

        userLocale = Locale.ENGLISH; // create a default voice

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                initTts(i);
            }
        });

        // automatically bring up the keyboard when the edittext is focused
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

    }

    private void initAccelerometer() {
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // TODO: 8/19/16 accelerometer is too sensitive
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                mAccelLast = mAccelCurrent;
                mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
                float delta = mAccelCurrent - mAccelLast; // change in acceleration
                mAccel = mAccel * 0.9f + delta;

                // erase textinput if acceleration is higher than a certain value (12)
                if (mAccel > 12) {
                    input.setText("");
                    vibrator.vibrate(10);
                }
            }
            @Override public void onAccuracyChanged(Sensor s, int i) {}
        };

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

    }

    /** the accelerometer should be deactivated onPause and activated onResume to save resources **/
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // parse the language display name into the language code to set our locale
        languageSearch: {
            for (Locale locale : availableLanguages) {
                if (locale.getDisplayName().equals(item.toString())) {
                    // set language on click
                    userLocale = new Locale(locale.toString());
                    currLang.setText(locale.getDisplayName());
                    break languageSearch;
                }
            }
        }
        // no error if the language is not available, tts simply makes no sound
        tts.setLanguage(userLocale);
        return super.onOptionsItemSelected(item);
    }

    // initialize our tts object
    public void initTts(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // setting speech language
            int result = tts.setLanguage(userLocale);
            // if device doesn't support language set above
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "User locale language not supported on this device", Toast.LENGTH_LONG).show();
            } else {
                currLang.setText(userLocale.getDisplayName());
                speakButton.setEnabled(true);
            }

            // add available languages to the menu
            Collection<String> languages = new TreeSet<>(Collator.getInstance());
            for(Locale locale : availableLanguages) {
                if(TextToSpeech.LANG_AVAILABLE == tts.isLanguageAvailable(locale) &&
                        !locale.getDisplayName().contains("(")) { // remove duplicates
                    languages.add(locale.getDisplayName());
                }
            }
            for (String language : languages) {
                menu.add(language);
            }
        }
        progressDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        // close the tts library
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
