package ajlp.mhacksxproject

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import com.twilio.chat.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

//asdf

class MainActivity : AppCompatActivity(), ClassifyTextMessageCallback {

    override fun onClassifyTextMessageFinished(message:Message, response: Boolean) {
        if(response) {
            textToSpeech = sayText(message.author + " sent you a message, ${message.messageBody}. Would you like to reply?")
        }
        else{
            sendAutoReply()
        }
    }

    private var mChatClient:ChatClient? = null
    private var mGeneralChannel:Channel? = null
    private val REQ_CODE_SPEECH_INPUT = 100
    var safetyButton = false
    var textToSpeech:TextToSpeech? = null
    private var state = 0
    var safetyModeOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()

        if(defaultSharedPreferences.getInt("filter", -1) == -1){
            defaultSharedPreferences.edit().putInt("filter", 1).apply()
        }

        if(defaultSharedPreferences.getString("autoReply", "") == ""){
            defaultSharedPreferences.edit().putString("autoReply", "I'm driving right now, ttyl.").apply()

        }

        retrieveAccessTokenfromServer()

        v_safety_button.setOnClickListener {
            safetyButton = !safetyButton
            setSafety(safetyButton)
        }

        v_chat_button.setOnClickListener{
            startActivity(Intent(applicationContext, ChatActivity::class.java))
            finish()
        }

        v_setting_button.setOnClickListener{
            startActivity(Intent(applicationContext, SettingsActivity::class.java))
        }

        val listener = object: LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d("Latitude", java.lang.Double.toString(location.latitude))
                Log.d("Longitude", java.lang.Double.toString(location.longitude))
                Log.d("Time", Date(location.time).toString())
                Log.d("Speed", java.lang.Float.toString(location.speed))
                if((location.speed)>2f && !safetyModeOn){
                    setSafety(true)
                    textToSpeech = sayText("Driving detected.  Switching to driving mode.", false)
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
        v_safety_enabled.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View): Unit{
                if(!safetyModeOn){
                    setSafety(true)
                    textToSpeech = sayText("Driving detected.  Switching to driving mode.", false)
                }


            }
        })
    }


    private fun setSafety(safetyEnabled:Boolean){
        if(safetyEnabled){
            v_safety_button.setImageResource(R.drawable.logo_on)
            v_safety_enabled.setText(R.string.mode_driving)
            safetyModeOn=true
        }else{
            v_safety_button.setImageResource(R.drawable.logo_off)
            v_safety_enabled.setText(R.string.mode_normal)
            safetyModeOn=false
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {

        }

    }

    fun sendAutoReply(){
        val message = Message.options().withBody(defaultSharedPreferences.getString("autoReply",""))
        val listener: CallbackListener<Message> =
                object : CallbackListener<Message>() {
                    override fun onSuccess(message: Message?) {
                        UserData.Messages.add(message!!)
                    }
                }
        mGeneralChannel!!.messages.sendMessage(message, listener)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {

                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val messageBody = result[0].toString()

                    if (state == 0) {
                        Log.d("test", messageBody);
                        Log.d("test", messageBody.contains("no").toString());

                        if (!messageBody.contains("no")){
                            state++
                            textToSpeech = sayText("Sure, go ahead.", true)
                        }else{
                            sendAutoReply()
                            textToSpeech = sayText("OK, I'll know next time.", false)
                        }
                    }else{
                        val message = Message.options().withBody(messageBody)
                        Log.d(ChatActivity.TAG, "Message created")
                        val listener: CallbackListener<Message> =
                                object : CallbackListener<Message>() {
                                    override fun onSuccess(message: Message?) {
                                        runOnUiThread {
                                            Toast.makeText(applicationContext, result[0], Toast.LENGTH_LONG).show()
                                            textToSpeech = sayText("OK, I replied " + result[0], false)
                                            UserData.Messages.add(message!!)
                                        }
                                    }
                                }
                        mGeneralChannel!!.messages.sendMessage(message, listener)
                        state = 0
                    }
                }
            }
        }
    }

    private fun sayText(text: String, startVoice: Boolean = true) : TextToSpeech{
        if(textToSpeech != null){
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        return TextToSpeech(applicationContext, object:TextToSpeech.OnInitListener{
            override fun onInit(status: Int) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech?.language = Locale.US
                    val map = HashMap<String, String>()
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")

                    textToSpeech?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                        override fun onStart(p0: String?) {
                        }

                        override fun onError(p0: String?) {
                        }

                        override fun onDone(p0: String?) {
                            Log.d(TAG, "Voice started")
                            if(startVoice) startVoiceInput()
                        }

                    })
                    textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH,map)

                }
            }

        })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        Log.d("CLIENT SHUTDOWN", "SHUTDOWN")
        super.onPause()
    }

    private fun retrieveAccessTokenfromServer() {
//        val deviceId = "myDevice"
//        val tokenURL = ChatActivity.SERVER_TOKEN_URL + "?device=" + deviceId
        val tokenURL = SERVER_TOKEN_URL + "/" + getString(R.string.username)
        Ion.with(this)
                .load(tokenURL)
                .asJsonObject()
                .setCallback(object: FutureCallback<JsonObject> {
                    override fun onCompleted(e:Exception?, result: JsonObject?) {
                        if (e == null)
                        {
                            val identity = result?.get("identity")?.getAsString()
                            val accessToken = result?.get("token")?.getAsString()
                            setTitle("Stacy")
                            val builder = ChatClient.Properties.Builder()
//                            builder.setRegion(ChatClient.ConnectionSt)
//                            builder.setSynchronizationStrategy(ChatClient.SynchronizationStrategy.ALL)
                            val props = builder.createProperties()
                            ChatClient.create(this@MainActivity, accessToken!!, props, mChatClientCallback)
                        }
                        else
                        {
                            Toast.makeText(this@MainActivity,
                                    "ERROR RECEIVING TOKEN", Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
                })
    }

    private fun joinChannel(channel: Channel) {
        Log.d(ChatActivity.TAG, "Joining Channel: " + channel.getUniqueName())
        channel.join(object: StatusListener() {
            override fun onSuccess() {
                mGeneralChannel = channel
                Log.d(ChatActivity.TAG, "Joined default channel")
                mGeneralChannel!!.addListener(mDefaultChannelListener)
            }
            override fun onError(errorInfo: ErrorInfo) {
                Log.e(ChatActivity.TAG, "Error joining channel: " + errorInfo.getMessage())
            }
        })
    }
    private val mChatClientCallback = object: CallbackListener<ChatClient>() {
        override fun onSuccess(chatClient:ChatClient) {
            mChatClient = chatClient
            loadChannels()
            Log.d(ChatActivity.TAG, "Success creating Twilio Chat Client")
        }
        override fun onError(errorInfo: ErrorInfo) {
            Log.e(ChatActivity.TAG, "Error creating Twilio Chat Client: " + errorInfo.getMessage())
        }
    }
    private val mDefaultChannelListener = object: ChannelListener {

        override fun onMemberUpdated(member: Member?, p1: Member.UpdateReason?) {
            Log.d(ChatActivity.TAG, "Member updated: " + member?.getIdentity())

        }

        override fun onMessageUpdated(message: Message?, p1: Message.UpdateReason?) {
            Log.d(ChatActivity.TAG, "Message updated: " + message?.getMessageBody())

        }

        override fun onMessageAdded(message: Message) {

            Log.d(ChatActivity.TAG, "Message added(MAIN)")
                Log.d(ChatActivity.TAG, "Author: " + message.author)
                if(mChatClient != null && mChatClient?.myIdentity != message.author && safetyButton){
                    if(defaultSharedPreferences.getInt("filter", -1) != -1){
                        when(defaultSharedPreferences.getInt("filter", -1)){
                            0 -> textToSpeech = sayText(message.author + " sent you a message, ${message.messageBody}. Would you like to reply?")
                            1 -> classifyTextMessage(message, this@MainActivity)
                            2 -> sendAutoReply()
                        }
                    }
                }
                // need to modify user interface elements on the UI thread
                UserData.Messages.add(message)
            }
        }
        override fun onMessageDeleted(message: Message) {
            Log.d(ChatActivity.TAG, "Message deleted")
        }
        override fun onMemberAdded(member: Member) {
            Log.d(ChatActivity.TAG, "Member added: " + member.getIdentity())
        }

        override fun onMemberDeleted(member: Member) {
            Log.d(ChatActivity.TAG, "Member deleted: " + member.getIdentity())
        }
        override fun onTypingStarted(member: Member) {
            Log.d(ChatActivity.TAG, "Started Typing: " + member.getIdentity())
        }
        override fun onTypingEnded(member: Member) {
            Log.d(ChatActivity.TAG, "Ended Typing: " + member.getIdentity())
        }
        override fun onSynchronizationChanged(channel: Channel) {
        }
    }

    private fun loadChannels() {
        mChatClient?.getChannels()?.getChannel(ChatActivity.DEFAULT_CHANNEL_NAME, object:CallbackListener<Channel>() {
            override fun onSuccess(channel:Channel) {
                if (channel != null)
                {
                    joinChannel(channel)
                }
                else
                {
                    mChatClient!!.getChannels().createChannel(ChatActivity.DEFAULT_CHANNEL_NAME,
                            Channel.ChannelType.PUBLIC, object:CallbackListener<Channel>() {
                        override fun onSuccess(channel:Channel) {
                            if (channel != null)
                            {
                                joinChannel(channel)
                            }
                        }
                        override fun onError(errorInfo:ErrorInfo) {
                            Log.e(ChatActivity.TAG, "Error creating channel: " + errorInfo.getMessage())
                        }
                    })
                }
            }
            override fun onError(errorInfo:ErrorInfo) {
                Log.e(ChatActivity.TAG, "Error retrieving channel: " + errorInfo.getMessage())
            }
        })
    }

    internal fun classifyTextMessage(message: Message, callback: ClassifyTextMessageCallback) {
        val mRequestQueue: RequestQueue

        // Instantiate the cache
        val cache = DiskBasedCache(cacheDir, 1024 * 1024) // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        val network = BasicNetwork(HurlStack())

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = RequestQueue(cache, network)

        // Start the queue
        mRequestQueue.start()

        val url = "http://35.202.120.11:81/classify"

        // Formulate the request and handle the response.
        val stringRequest = object : StringRequest(Request.Method.POST, url,
                object : Response.Listener<String> {
                    override fun onResponse(response: String) {
                        val resp = Integer.parseInt(response) == 1
                        callback.onClassifyTextMessageFinished(message, resp)
                    }
                },
                object : Response.ErrorListener {
                    override fun onErrorResponse(error: VolleyError) {
                        error.printStackTrace()
                    }
                }) {
            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params.put("text", message.messageBody)

                return params
            }
        }

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest)
    }


    companion object {
        /*
     Change this URL to match the token URL for your quick start server
     Download the quick start server from:
     https://www.twilio.com/docs/api/ip-messaging/guides/quickstart-js
     */
        internal val SERVER_TOKEN_URL = "http://35.202.120.11/mhacks_chad/token"
        internal val DEFAULT_CHANNEL_NAME = "general"
        internal val TAG = "TwilioChat"
    }


}
