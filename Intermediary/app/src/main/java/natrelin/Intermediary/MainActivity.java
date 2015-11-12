package natrelin.Intermediary;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity  implements TextToSpeech.OnInitListener {

    // text to speech
    private EditText input;
    private TextToSpeech tts;
    private Button speak;
    private Button stop;
    private Locale userLocale;
    private Locale[] availableLanguages;

    // accelerometer
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;

    private final String TAG = "MAINACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAccelerometer(); // set up accelerometer

        availableLanguages = Locale.getAvailableLocales(); // get all of the available locales
        userLocale = Locale.JAPAN; // create a default voice

        tts = new TextToSpeech(this, this);

        input = (EditText) findViewById(R.id.edittext);
        // automatically bring up the keyboard when the edittext is focused
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        speak = (Button) findViewById(R.id.speak);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGreater21(input.getText().toString());
                } else {
                    ttsUnder20(input.getText().toString());
                }
            }
        });

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tts.isSpeaking()) {
                    tts.stop();
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void initAccelerometer() {
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        /*mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;*/
    }

    /** the accelerometer should be deactivated onPause and activated onResume to save resources **/
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(sensorEventListener);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        for(Locale locale : availableLanguages) {
            // TODO check if the language is installed on the device, if so...
            //Setting speech language
            //If your device supports language you set above
            //if (tts.setLanguage(locale) == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE)
                menu.add(locale.toString());
        }

        /*menu.add(userLocale.toString());
        menu.add((Locale.US).toString());*/

        return true;
        /** onInit is called after this function... **/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // set language on click
        userLocale = new Locale(item.toString());
        tts.setLanguage(userLocale); // no error if the language is not available, installed languages work fine
        return super.onOptionsItemSelected(item);
    }

    // initialize our tts object
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //Setting speech language
            int result = tts.setLanguage(userLocale);
            //If your device doesn't support language you set above
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                //Cook simple toast message with message
                Toast.makeText(this, "Language not supported", Toast.LENGTH_LONG).show();
                Log.e("TTS", "Language is not supported");
            }
            //Enable the button
            else {
                speak.setEnabled(true);
            }
            //TTS is not initialized properly
        } else {
            Toast.makeText(this, "TTS Initilization Failed", Toast.LENGTH_LONG).show();
            Log.e("TTS", "Initilization Failed");
        }
    }

    @Override
    protected void onDestroy() {

        // E/ViewRootImpl: sendUserActionEvent() mView == null // device issue, not app related

        // TODO: 11/12/15 fix InputConnection warnings
        // W/IInputConnectionWrapper: showStatusIcon on inactive InputConnection
        // W/IInputConnectionWrapper: getExtractedText on inactive InputConnection
        // W/IInputConnectionWrapper: beginBatchEdit on inactive InputConnection
        // W/IInputConnectionWrapper: endBatchEdit on inactive InputConnection
        // W/IInputConnectionWrapper: finishComposingText on inactive InputConnection

        //Close the Text to Speech Library
        if(tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }
        super.onDestroy();
    }
}
