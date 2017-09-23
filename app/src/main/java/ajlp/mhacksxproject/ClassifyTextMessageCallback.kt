package ajlp.mhacksxproject

/**
 * Created by alessiosymons on 2017-09-23.
 */
interface ClassifyTextMessageCallback {
    fun onClassifyTextMessageFinished(message:String, response: Boolean)
}