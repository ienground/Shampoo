package zone.ien.shampoo.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DetailActivity
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.adapter.DashboardAdapter
import zone.ien.shampoo.callback.DashboardCallback
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.utils.Dlog

class MainDashboardFragment : Fragment() {

    private lateinit var binding: FragmentMainDashboardBinding

    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>
    private var deviceDatabase: DeviceDatabase? = null

    private var dashboardCallback = object: DashboardCallback {
        override fun callback(position: Int, id: Long) {
            startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                putExtra(IntentKey.DATA_ID, id)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_dashboard, container, false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_add -> {
                        startActivity(Intent(requireContext(), DeviceAddActivity::class.java))
//                        barcodeLauncher.launch(ScanOptions().apply {
//                            setOrientationLocked(true)
//                        })
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        deviceDatabase = DeviceDatabase.getInstance(requireContext())

        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll() as ArrayList?
            data?.let {
                val adapter = DashboardAdapter(it).apply {
                    setClickCallback(dashboardCallback)
                }
                binding.list.adapter = adapter
            }
        }


//        val adapter = DashboardAdapter(list)
//        binding.list.adapter = adapter

        // barcode
        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Dlog.d(TAG, "result null")
            } else {
                Dlog.d(TAG, "Scanned ${result.contents}")
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainDashboardFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}