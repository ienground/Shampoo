package zone.ien.shampoo.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.opencsv.CSVReader
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.databinding.FragmentDeviceAddGuideBinding
import zone.ien.shampoo.databinding.FragmentDeviceAddInfoBinding
import zone.ien.shampoo.databinding.FragmentDeviceAddSettingBinding
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import java.io.InputStreamReader

class DeviceAddSettingFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddSettingBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null
    private var productList: ArrayList<Array<String>> = arrayListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_setting, container, false)

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

        binding.inputName.editText?.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                callbackListener?.setFinishButtonEnabled(p0.toString() != "" && binding.inputRoom.editText?.text?.toString() != "")
                if (p0.toString() != "") {
                    DeviceAddActivity.deviceName = p0.toString()
                }
            }
        })
        binding.inputRoom.editText?.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                callbackListener?.setFinishButtonEnabled(p0.toString() != "" && binding.inputName.editText?.text?.toString() != "")
                if (p0.toString() != "") {
                    DeviceAddActivity.deviceRoom = p0.toString().toInt()
                }
            }
        })

    }

    fun setCallbackListener(callbackListener: DeviceAddActivityCallback?) {
        this.callbackListener = callbackListener
    }

    override fun onResume() {
        super.onResume()
        val product = productList.find { it[0].replace(" ", "") == DeviceAddActivity.deviceBarcode }
        callbackListener?.setButtonEnabled(isPrev = true, isEnabled = false)
        callbackListener?.setFinishButtonShow(true)
        callbackListener?.setFinishButtonEnabled(false)

//        product?.let {
//            binding.tvContentType.text = it[1]
//            binding.tvContentCapacity.text = it[3]
//        }
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
        fun newInstance() = DeviceAddSettingFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}