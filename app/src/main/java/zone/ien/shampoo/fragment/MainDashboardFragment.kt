package zone.ien.shampoo.fragment

import android.content.Context
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.adapter.DashboardAdapter
import zone.ien.shampoo.data.DashboardEntity
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.utils.Dlog

class MainDashboardFragment : Fragment() {

    private lateinit var binding: FragmentMainDashboardBinding

    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_dashboard, container, false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_add -> {
                        barcodeLauncher.launch(ScanOptions().apply {
                            setOrientationLocked(true)
                        })
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val list = arrayListOf(
            DashboardEntity("Hello", 70, 0, 0),
            DashboardEntity("Hello2", 20, 1, 0),
            DashboardEntity("Hello3", 40, -1, 0),
            DashboardEntity("Hello4", 50, 3, 0),
        )

        val adapter = DashboardAdapter(list)
        binding.list.adapter = adapter

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