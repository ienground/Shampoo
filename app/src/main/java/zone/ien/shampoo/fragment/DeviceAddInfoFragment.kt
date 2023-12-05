package zone.ien.shampoo.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.opencsv.CSVReader
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.databinding.FragmentDeviceAddGuideBinding
import zone.ien.shampoo.databinding.FragmentDeviceAddInfoBinding
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.room.DeviceDao
import zone.ien.shampoo.utils.Dlog
import java.io.InputStreamReader

class DeviceAddInfoFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddInfoBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null
    private var productList: ArrayList<Array<String>> = arrayListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_info, container, false)

//        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val assetManager = requireActivity().assets
        val inputStream = assetManager.open("bubble_datasheet.csv")
        val csvReader = CSVReader(InputStreamReader(inputStream, "EUC-KR"))
        productList.addAll(csvReader.readAll())
    }

    fun setCallbackListener(callbackListener: DeviceAddActivityCallback?) {
        this.callbackListener = callbackListener
    }

    override fun onResume() {
        super.onResume()
        Dlog.d(TAG, "onResume ${DeviceAddActivity.deviceBarcode} ${DeviceAddActivity.deviceAddress}")
        val product = productList.find { it[0].replace(" ", "") == DeviceAddActivity.deviceBarcode }
        callbackListener?.setButtonEnabled(isPrev = true, isEnabled = false)
        callbackListener?.setButtonEnabled(isPrev = false, isEnabled = true)

        product?.let {
            DeviceAddActivity.deviceProduct = it[1]
            DeviceAddActivity.deviceType = it[2].toInt()
            DeviceAddActivity.deviceMax = it[3].toInt()
            binding.tvContentType.text = it[1]
            binding.tvContentCapacity.text = it[3]

            if (it[6] != "") {
                Dlog.d(TAG, "link: ${it[6]}")
                Glide.with(this)
                    .load(it[6])
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_error)
                    .into(binding.imgPreview)
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
        fun newInstance() = DeviceAddInfoFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}