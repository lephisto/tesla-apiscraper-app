package to.mephis.apiscrapercontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

public class Settings extends AppCompatActivity {

    private EditText mBtName;
    private EditText mBtTimeout;
    private SeekBar mBtSeekbar;
    private EditText mApiUrl;
    private EditText mApiKey;

    private static Settings instance;
    public static Settings getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        instance = this;
        setContentView(R.layout.activity_settings);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mBtName = (EditText) findViewById(R.id.btMac);
        mBtTimeout = (EditText) findViewById(R.id.btTimeout);
        mApiUrl = (EditText) findViewById(R.id.apiurl);
        mApiKey = (EditText) findViewById(R.id.apikey);
        mBtSeekbar = (SeekBar) findViewById(R.id.seekBarBTTimeout);

        mBtName.setText(getIntent().getStringExtra("btname"));
        mBtTimeout.setText(getIntent().getStringExtra("bttimeout"));
        mBtSeekbar.setProgress(Integer.parseInt(getIntent().getStringExtra("bttimeout")));
        mApiUrl.setText(getIntent().getStringExtra("apiurl"));
        mApiKey.setText(getIntent().getStringExtra("apikey"));

        Button mBtnSaveSettings = (Button) findViewById(R.id.btn_saveSettings);
        mBtnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
                // returing result back
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result", "Settings saved..");
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        mBtSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                set_btTimeout(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    private void set_btTimeout(int howMany) {
        String newval = String.valueOf(howMany);
        mBtTimeout.setText(newval);
    }

    // react on Android Back Button
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("result", "cancel..");
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }


    // react on Toolbar Back
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

     public void saveSettings() {
        //Save Settings
        String btmac = mBtName.getText().toString();
        writeSharedPrefString(getIntent().getStringExtra("pref_btMac"),btmac,"myStore");
        String bttimeout = mBtTimeout.getText().toString();
        writeSharedPrefString(getIntent().getStringExtra("pref_btTimeout"),bttimeout,"myStore");
        String apiurl = mApiUrl.getText().toString();
        writeSharedPrefString(getIntent().getStringExtra("pref_apiUrl"),apiurl,"myStore");
        String apikey = mApiKey.getText().toString();
        writeSharedPrefString(getIntent().getStringExtra("pref_apiKey"),apikey,"myStore");
    }

    public void writeSharedPrefString(String pref, String value, String store){
        SharedPreferences.Editor editor = getSharedPreferences(pref,0).edit();
        editor.putString(store,value);
        editor.commit();
    }
}
