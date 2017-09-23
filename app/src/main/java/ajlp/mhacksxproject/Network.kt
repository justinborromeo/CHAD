package ajlp.mhacksxproject


import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.android.volley.RequestQueue

/**
 * Created by Root on 2017-08-18.
 */
class Network private constructor(context: Context) {
    private var mRequestQueue: RequestQueue? = null

    init {
        mCtx = context
        mRequestQueue = requestQueue
    }


    val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(mCtx?.applicationContext)
            }
            return mRequestQueue!!
        }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    companion object {
        private var mInstance: Network? = null
        private var mCtx: Context? = null

        @Synchronized fun getInstance(context: Context): Network {
            if (mInstance == null) {
                mInstance = Network(context)
            }
            return mInstance!!
        }
    }
}