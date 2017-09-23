package ajlp.mhacksxproject

import android.os.Bundle
import android.provider.Settings.Secure
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import com.twilio.chat.CallbackListener
import com.twilio.chat.Channel
import com.twilio.chat.ChannelListener
import com.twilio.chat.ChatClient
import com.twilio.chat.ErrorInfo
import com.twilio.chat.Member
import com.twilio.chat.Message
import com.twilio.chat.StatusListener
import kotlinx.android.synthetic.main.activity_chat.*
import java.util.ArrayList

class ChatActivity:AppCompatActivity() {

    private var mMessagesRecyclerView:RecyclerView? = null
    private var mMessagesAdapter:MessagesAdapter? = null
    private var mMessages = ArrayList<Message>()
    private var mWriteMessageEditText:EditText? = null
    private var mSendChatMessageButton:Button? = null
    private var mChatClient:ChatClient? = null
    private var mGeneralChannel:Channel? = null

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        mMessagesRecyclerView = messagesRecyclerView
        val layoutManager = LinearLayoutManager(this)
        // for a chat app, show latest at the bottom
        layoutManager.setStackFromEnd(true)
        mMessagesRecyclerView?.setLayoutManager(layoutManager)
        mMessagesAdapter = MessagesAdapter()
        mMessagesRecyclerView?.setAdapter(mMessagesAdapter)
        mWriteMessageEditText = writeMessageEditText
        mSendChatMessageButton = sendChatMessageButton
        mSendChatMessageButton?.setOnClickListener(object:View.OnClickListener {
            override fun onClick(view:View) {
                if (mGeneralChannel != null)
                {
                    val messageBody = (mWriteMessageEditText as EditText?)?.text.toString()
                    val message = Message.options().withBody(messageBody)
                    Log.d(TAG, "Message created")
                    val listener: CallbackListener<Message> =
                            object:CallbackListener<Message>() {
                                override fun onSuccess(p0: Message?) {
                                    runOnUiThread(object : Runnable {
                                        public override fun run() {
                                            // need to modify user interface elements on the UI thread
                                            (mWriteMessageEditText as TextView).text = ""
                                        }
                                    })
                                }
                            }
                    mGeneralChannel!!.messages.sendMessage(message, listener)
                }
            }
        })
        retrieveAccessTokenfromServer()
    }
    private fun retrieveAccessTokenfromServer() {
        val deviceId = "myDevice"
        val tokenURL = SERVER_TOKEN_URL + "?device=" + deviceId
        Ion.with(this)
                .load(tokenURL)
                .asJsonObject()
                .setCallback(object:FutureCallback<JsonObject> {
                    override fun onCompleted(e:Exception?, result:JsonObject?) {
                        if (e == null)
                        {
                            val identity = result?.get("identity")?.getAsString()
                            val accessToken = result?.get("token")?.getAsString()
                            setTitle(identity)
                            val builder = ChatClient.Properties.Builder()
//                            builder.setRegion(ChatClient.ConnectionSt)
//                            builder.setSynchronizationStrategy(ChatClient.SynchronizationStrategy.ALL)
                            val props = builder.createProperties()
                            ChatClient.create(this@ChatActivity, accessToken!!, props, mChatClientCallback)
                        }
                        else
                        {
                            Toast.makeText(this@ChatActivity,
                                    "ERROR RECEIVING TOKEN", Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
                })
    }
    private fun loadChannels() {
        mChatClient?.getChannels()?.getChannel(DEFAULT_CHANNEL_NAME, object:CallbackListener<Channel>() {
            override fun onSuccess(channel:Channel) {
                if (channel != null)
                {
                    joinChannel(channel)
                }
                else
                {
                    mChatClient!!.getChannels().createChannel(DEFAULT_CHANNEL_NAME,
                            Channel.ChannelType.PUBLIC, object:CallbackListener<Channel>() {
                        override fun onSuccess(channel:Channel) {
                            if (channel != null)
                            {
                                joinChannel(channel)
                            }
                        }
                        override fun onError(errorInfo:ErrorInfo) {
                            Log.e(TAG, "Error creating channel: " + errorInfo.getMessage())
                        }
                    })
                }
            }
            override fun onError(errorInfo:ErrorInfo) {
                Log.e(TAG, "Error retrieving channel: " + errorInfo.getMessage())
            }
        })
    }
    private fun joinChannel(channel:Channel) {
        Log.d(TAG, "Joining Channel: " + channel.getUniqueName())
        channel.join(object:StatusListener() {
            override fun onSuccess() {
                mGeneralChannel = channel
                Log.d(TAG, "Joined default channel")
                mGeneralChannel!!.addListener(mDefaultChannelListener)
            }
            override fun onError(errorInfo:ErrorInfo) {
                Log.e(TAG, "Error joining channel: " + errorInfo.getMessage())
            }
        })
    }
    private val mChatClientCallback = object:CallbackListener<ChatClient>() {
        override fun onSuccess(chatClient:ChatClient) {
            mChatClient = chatClient
            loadChannels()
            Log.d(TAG, "Success creating Twilio Chat Client")
        }
        override fun onError(errorInfo:ErrorInfo) {
            Log.e(TAG, "Error creating Twilio Chat Client: " + errorInfo.getMessage())
        }
    }
    private val mDefaultChannelListener = object:ChannelListener {

        override fun onMemberUpdated(member: Member?, p1: Member.UpdateReason?) {
            Log.d(TAG, "Member updated: " + member?.getIdentity())

        }

        override fun onMessageUpdated(message: Message?, p1: Message.UpdateReason?) {
            Log.d(TAG, "Message updated: " + message?.getMessageBody())

        }

        override fun onMessageAdded(message:Message) {
            Log.d(TAG, "Message added")
            runOnUiThread(object:Runnable {
                public override fun run() {
                    // need to modify user interface elements on the UI thread
                    mMessages.add(message)
                    mMessagesAdapter?.notifyDataSetChanged()
                }
            })
        }
        override fun onMessageDeleted(message:Message) {
            Log.d(TAG, "Message deleted")
        }
        override fun onMemberAdded(member:Member) {
            Log.d(TAG, "Member added: " + member.getIdentity())
        }

        override fun onMemberDeleted(member:Member) {
            Log.d(TAG, "Member deleted: " + member.getIdentity())
        }
        override fun onTypingStarted(member:Member) {
            Log.d(TAG, "Started Typing: " + member.getIdentity())
        }
        override fun onTypingEnded(member:Member) {
            Log.d(TAG, "Ended Typing: " + member.getIdentity())
        }
        override fun onSynchronizationChanged(channel:Channel) {
        }
    }
    internal inner class MessagesAdapter:RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return mMessages.size
        }

        internal inner class ViewHolder(textView:TextView):RecyclerView.ViewHolder(textView) {
            var mMessageTextView:TextView
            init{
                mMessageTextView = textView
            }
        }
        override fun onCreateViewHolder(parent:ViewGroup,
                               viewType:Int):MessagesAdapter.ViewHolder {
            val messageTextView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_text_view, parent, false) as TextView
            return ViewHolder(messageTextView)
        }
        override fun onBindViewHolder(holder:ViewHolder, position:Int) {
            val message = mMessages.get(position)
            val messageText = String.format("%s: %s", message.getAuthor(), message.getMessageBody())
            holder.mMessageTextView.setText(messageText)
        }

    }
    companion object {
        /*
     Change this URL to match the token URL for your quick start server
     Download the quick start server from:
     https://www.twilio.com/docs/api/ip-messaging/guides/quickstart-js
     */
        internal val SERVER_TOKEN_URL = "http://localhost:8000/token.php"
        internal val DEFAULT_CHANNEL_NAME = "general"
        internal val TAG = "TwilioChat"
    }
}