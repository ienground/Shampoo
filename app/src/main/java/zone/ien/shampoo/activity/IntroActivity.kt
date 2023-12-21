package zone.ien.shampoo.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.IntentKey

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val type = intent.getIntExtra(IntentKey.NOTI_TYPE, -1)
        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        startActivity(Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(IntentKey.NOTI_TYPE, type)
            putExtra(IntentKey.ITEM_ID, id)
        })
        finish()
    }
}
