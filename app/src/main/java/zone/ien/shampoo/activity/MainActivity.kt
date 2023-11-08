package zone.ien.shampoo.activity

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import zone.ien.shampoo.R
import zone.ien.shampoo.databinding.ActivityMainBinding
import zone.ien.shampoo.fragment.MainDashboardFragment

const val TAG = "ShampooTAG"

class MainActivity : AppCompatActivity(),
        MainDashboardFragment.OnFragmentInteractionListener
{

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this

        loadFragment(MainDashboardFragment.newInstance())

        binding.navView.setOnItemSelectedListener {
            loadFragment(when (it.itemId) {
                R.id.navigation_dashboard -> MainDashboardFragment.newInstance()
                R.id.navigation_store -> MainDashboardFragment.newInstance()
                else -> MainDashboardFragment.newInstance()
            })
        }
    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            return true
        }
        return false
    }

    override fun onFragmentInteraction(uri: Uri) {}
}