package ajlp.mhacksxproject

import com.twilio.chat.Message

/**
 * Created by alessiosymons on 2017-09-23.
 */
interface ClassifyTextMessageCallback {
    fun onClassifyTextMessageFinished(message: Message, response: Boolean)
}