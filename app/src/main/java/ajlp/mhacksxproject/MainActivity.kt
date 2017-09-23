package ajlp.mhacksxproject

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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.*
import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import com.twilio.chat.*
import java.util.*




class MainActivity : AppCompatActivity() {


    private var mChatClient:ChatClient? = null
    private var mGeneralChannel:Channel? = null
    private val REQ_CODE_SPEECH_INPUT = 100
    var safetyButton = false
    var textToSpeech:TextToSpeech? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()

        v_safety_button.setOnClickListener {
            safetyButton = !safetyButton
            setSafety(safetyButton)
        }

        v_chat_button.setOnClickListener{
            startActivity(Intent(applicationContext, ChatActivity::class.java))
        }

        retrieveAccessTokenfromServer()
    }


    private fun setSafety(safetyEnabled:Boolean){
        if(safetyEnabled){
            v_safety_button.text = resources.getText(R.string.off)
            v_safety_enabled.text = resources.getText(R.string.safety_enabled)
            v_safety_enabled.textColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        }else{
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
                    val messageBody = result[0].toString()
                    val message = Message.options().withBody(messageBody)
                    Log.d(ChatActivity.TAG, "Message created")
                    val listener: CallbackListener<Message> =
                            object:CallbackListener<Message>() {
                                override fun onSuccess(p0: Message?) {
                                    runOnUiThread {
                                        Toast.makeText(applicationContext, result[0], Toast.LENGTH_LONG).show()
                                        textToSpeech = sayText("Message sent", false)
                                    }
                                }
                            }
                    mGeneralChannel!!.messages.sendMessage(message, listener)
                }
            }
        }
    }

    private fun sayText(text : String, startVoice : Boolean = true) : TextToSpeech{
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
        retrieveAccessTokenfromServer()
        super.onResume()
    }

    override fun onPause() {
        if(textToSpeech != null){
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        mChatClient?.shutdown()
        super.onPause()
    }

    private fun retrieveAccessTokenfromServer() {
//        val deviceId = "myDevice"
//        val tokenURL = ChatActivity.SERVER_TOKEN_URL + "?device=" + deviceId
        val tokenURL = SERVER_TOKEN_URL + "/Chad"
        Ion.with(this)
                .load(tokenURL)
                .asJsonObject()
                .setCallback(object: FutureCallback<JsonObject> {
                    override fun onCompleted(e:Exception?, result: JsonObject?) {
                        if (e == null)
                        {
                            val identity = result?.get("identity")?.getAsString()
                            val accessToken = result?.get("token")?.getAsString()
                            setTitle(identity)
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
            Log.d(ChatActivity.TAG, "Message added")
            runOnUiThread(object:Runnable {
                public override fun run() {
                    Log.d(ChatActivity.TAG, "Author: " + message.author)
                    if(mChatClient != null && mChatClient?.myIdentity != message.author && safetyButton){
                        textToSpeech = sayText(message.messageBody)
                    }

                    // need to modify user interface elements on the UI thread
                    UserData.Messages.add(message)
                }
            })
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

    companion object {
        /*
     Change this URL to match the token URL for your quick start server
     Download the quick start server from:
     https://www.twilio.com/docs/api/ip-messaging/guides/quickstart-js
     */
        internal val SERVER_TOKEN_URL = "http://35.202.120.11/token"
        internal val DEFAULT_CHANNEL_NAME = "general"
        internal val TAG = "TwilioChat"
    }
}