package ajlp.mhacksxproject

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast

/**
 * Created by alessiosymons on 2017-09-23.
 */
class TextReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val SMS_EXTRA_NAME = "pdus"
        val extras = intent?.extras;
        var messages = "";

        if ( extras != null )
        {
            // Get received SMS array
            val smsExtra : Array<Object> =  extras.get( SMS_EXTRA_NAME ) as Array<Object>

            // Get ContentResolver object for pushing encrypted SMS to the incoming folder
            val contentResolver = context?.getContentResolver();

            for ( i in 0..smsExtra.size-1)
            {
                val sms = SmsMessage.createFromPdu(smsExtra[i] as ByteArray)

                val body = sms.getMessageBody().toString();
                val address = sms.getOriginatingAddress();

                messages += "SMS from " + address + " :\n";
                messages += body + "\n";

                // Here you can add any your code to work with incoming SMS
                // I added encrypting of all received SMS

//                putSmsToDatabase( contentResolver, sms );
            }

            // Display SMS message
            Toast.makeText( context, messages, Toast.LENGTH_SHORT ).show()

            this.abortBroadcast()
        }

    }
}