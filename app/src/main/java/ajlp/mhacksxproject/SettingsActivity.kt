package ajlp.mhacksxproject

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.defaultSharedPreferences

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if(defaultSharedPreferences.getInt("filter", -1) != -1){
            s_user_importance_setting.text = when(defaultSharedPreferences.getInt("filter", -1)) {
                0 -> "No Filter"
                1 -> "Smart Text Filter"
                else -> "All Filtered"
            }
            s_user_importance_seekbar.progress = defaultSharedPreferences.getInt("filter", -1)
        }

        if(defaultSharedPreferences.getString("autoReply", "") != ""){
            s_auto_reply_setting.setText(defaultSharedPreferences.getString("autoReply", ""))
        }

        s_auto_reply_setting.addTextChangedListener(object: TextWatcher{
            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                defaultSharedPreferences.edit().putString("autoReply", text.toString()).apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        s_user_importance_seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onProgressChanged(seek: SeekBar?, progress: Int, p2: Boolean) {
                defaultSharedPreferences.edit().putInt("filter", progress).apply()
                s_user_importance_setting.text = when(progress) {
                    0 -> "No Filter"
                    1 -> "Smart Text Filter"
                    else -> "All Filtered"
                }
            }

        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(android.R.id.home == item?.itemId){
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
