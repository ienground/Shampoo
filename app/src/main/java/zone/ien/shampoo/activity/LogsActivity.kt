package zone.ien.shampoo.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.databinding.ActivityLogsBinding
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.MyUtils

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding

    private var deviceLogDatabase: DeviceLogDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = DataBindingUtil.setContentView(this, R.layout.activity_logs)

//        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, getAttrColor(theme, Colors.colorOnSecondaryContainer)) })

        deviceLogDatabase = DeviceLogDatabase.getInstance(this)

        val type = intent.getIntExtra(IntentKey.LOG_TYPE, -1)

    }
}