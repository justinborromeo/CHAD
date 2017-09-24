package ajlp.mhacksxproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.textColor
import android.speech.RecognizerIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import java.util.*
import android.os.Handler
import android.util.Log
import kotlin.concurrent.fixedRateTimer
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider
import android.os.Message


class MainActivity : AppCompatActivity() {
    private val REQ_CODE_SPEECH_INPUT = 100
    private val mVoiceInputTv: TextView? = null
    private val mSpeakBtn: ImageButton? = null
    var safetyButton = false
    var textToSpeech:TextToSpeech? =null
    var locationManager: LocationManager? = null
    var locationProvider: LocationProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()

        v_safety_button.setOnClickListener {
            safetyButton = !safetyButton
            setSafety(safetyButton)
        }

        v_chat_button.setOnClickListener {
            startActivity(Intent(applicationContext, ChatActivity::class.java))
        }
        val listener = object: LocationListener{
            override fun onLocationChanged(location: Location) {
                Log.d("Latitude", java.lang.Double.toString(location.latitude))
                Log.d("Longitude", java.lang.Double.toString(location.longitude))
                Log.d("Time", Date(location.time).toString())
                Log.d("Speed", java.lang.Float.toString(location.speed))
                if((location.speed)>2f){
                    setSafety(true)
                }

            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {

            }

            override fun onProviderEnabled(s: String) {

            }

            override fun onProviderDisabled(s: String) {

            }
        }
        var mLocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                3000,          // 10-second interval.
                0f,             // 10 meters.
                listener);
    }

    fun setSafety(safetyEnabled:Boolean){
        if(safetyEnabled){
            startVoiceInput()
            v_safety_button.text = resources.getText(R.string.off)
            v_safety_enabled.text = resources.getText(R.string.safety_enabled)
            v_safety_enabled.textColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        }else{
            textToSpeech = sayText("Hey chad")
            v_safety_button.text = resources.getText(R.string.on)
            v_safety_enabled.text = resources.getText(R.string.safety_disabled)
            v_safety_enabled.textColor = ContextCompat.getColor(applicationContext, R.color.colorWarning)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?")
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    Toast.makeText(applicationContext, result[0], Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sayText(text : String) : TextToSpeech{
        if(textToSpeech != null){
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        return TextToSpeech(applicationContext, object:TextToSpeech.OnInitListener{
            override fun onInit(status: Int) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech?.language = Locale.ITALIAN
                    textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH,null)
                }
            }

        })
    }

    override fun onPause() {
        if(textToSpeech != null){
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onPause()
    }
}
