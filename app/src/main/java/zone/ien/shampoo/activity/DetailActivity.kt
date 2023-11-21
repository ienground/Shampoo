package zone.ien.shampoo.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.databinding.ActivityDetailBinding
import zone.ien.shampoo.room.DeviceDatabase

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private var deviceDatabase: DeviceDatabase? = null
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        deviceDatabase = DeviceDatabase.getInstance(this)

        val id = intent.getLongExtra(IntentKey.DATA_ID, -1)
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = deviceDatabase?.getDao()?.get(id)
                data?.let {
                    binding.tvContentLocation.text = it.room.toString()
                    binding.tvContentType.text = it.product
                    binding.tvContentCapacity.text = it.max.toString()
                }
            }
        }

    }
}