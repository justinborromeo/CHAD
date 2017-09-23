package ajlp.mhacksxproject

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.textColor

class MainActivity : AppCompatActivity() {

    var safetyButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()
        v_safety_button.setOnClickListener {
            safetyButton = !safetyButton
            setSafety(safetyButton)
        }
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
}
