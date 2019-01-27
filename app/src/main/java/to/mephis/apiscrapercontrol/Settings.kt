package to.mephis.apiscrapercontrol

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar

class Settings : AppCompatActivity() {

    private var mBtName: EditText? = null
    private var mBtTimeout: EditText? = null
    private var mBtSeekbar: SeekBar? = null
    private var mApiUrl: EditText? = null
    private var mApiKey: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        instance = this
        setContentView(R.layout.activity_settings)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        mBtName = findViewById<View>(R.id.btMac) as EditText
        mBtTimeout = findViewById<View>(R.id.btTimeout) as EditText
        mApiUrl = findViewById<View>(R.id.apiurl) as EditText
        mApiKey = findViewById<View>(R.id.apikey) as EditText
        mBtSeekbar = findViewById<View>(R.id.seekBarBTTimeout) as SeekBar

        mBtName!!.setText(intent.getStringExtra("btname"))
        mBtTimeout!!.setText(intent.getStringExtra("bttimeout"))
        mBtSeekbar!!.progress = Integer.parseInt(intent.getStringExtra("bttimeout"))
        mApiUrl!!.setText(intent.getStringExtra("apiurl"))
        mApiKey!!.setText(intent.getStringExtra("apikey"))

        val mBtnSaveSettings = findViewById<View>(R.id.btn_saveSettings) as Button
        mBtnSaveSettings.setOnClickListener {
            saveSettings()
            // returing result back
            val resultIntent = Intent()
            resultIntent.putExtra("result", "Settings saved..")
            //setResult(Activity.RESULT_OK, resultIntent)
            setResult(Activity.RESULT_OK,resultIntent)
            finish()
        }

        mBtSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                set_btTimeout(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })


    }

    private fun set_btTimeout(howMany: Int) {
        val newval = howMany.toString()
        mBtTimeout!!.setText(newval)
    }

    // react on Android Back Button
    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra("result", "cancel..")
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }


    // react on Toolbar Back
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return false
    }

    fun saveSettings() {
        //Save Settings
        val btmac = mBtName!!.text.toString()
        writeSharedPrefString(intent.getStringExtra("pref_btMac"), btmac, "myStore")
        val bttimeout = mBtTimeout!!.text.toString()
        writeSharedPrefString(intent.getStringExtra("pref_btTimeout"), bttimeout, "myStore")
        val apiurl = mApiUrl!!.text.toString()
        writeSharedPrefString(intent.getStringExtra("pref_apiUrl"), apiurl, "myStore")
        val apikey = mApiKey!!.text.toString()
        writeSharedPrefString(intent.getStringExtra("pref_apiKey"), apikey, "myStore")
    }

    fun writeSharedPrefString(pref: String, value: String, store: String) {
        val editor = getSharedPreferences(pref, 0).edit()
        editor.putString(store, value)
        editor.commit()
    }

    companion object {

        var instance: Settings? = null
            private set
    }
}
