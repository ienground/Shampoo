package zone.ien.shampoo.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.databinding.FragmentDeviceAddGuideBinding
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.utils.Dlog
import java.util.Date
import kotlin.math.abs

class DeviceAddGuideFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddGuideBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null

    private var determinedValue = -1
    private var determinedTime = System.currentTimeMillis()
    private var thresholdTime = 5 * 1000
    private var isMeasuring = true
    private var values = arrayListOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_guide, container, false)

//        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val value = intent.getIntExtra(IntentKey.BLE_CHAR_CHANGED, -1)
                if (isMeasuring) Dlog.d(TAG, "value $value determinedValue $determinedValue time ${Date(determinedTime)}")
                if (isMeasuring) {
                    binding.tvWeight.text = value.toString()
                    values.add(value)
                    if (determinedValue != 0) { // 어쨌든 값이 정해졌음 - 10초 동안 determinedValue가 유지되면 그 값으로 결정.
                        if (abs(determinedValue - value) > 20) {
                            determinedValue = 0
                            values.clear()
                        } else if (System.currentTimeMillis() - determinedTime >= thresholdTime) { // 값 결정
                            isMeasuring = false
                            binding.progress.visibility = View.INVISIBLE
                            binding.icCheck.visibility = View.VISIBLE
                            binding.tvState.text = getString(R.string.measurement_completed)
                            binding.tvWeight.text = values.average().toString()
                            DeviceAddActivity.deviceCapacity = values.average()
                            callbackListener?.setButtonEnabled(isPrev = false, isEnabled = true)
                        }
                    } else {
                        determinedValue = value
                        determinedTime = System.currentTimeMillis()
                        values.clear()
                    }
                }
            }
        }, IntentFilter(IntentID.BLE_DEVICE), Context.RECEIVER_EXPORTED)

    }

    fun setCallbackListener(callbackListener: DeviceAddActivityCallback?) {
        this.callbackListener = callbackListener
    }

    override fun onResume() {
        super.onResume()
        callbackListener?.setButtonEnabled(isPrev = true, isEnabled = true)
        callbackListener?.setButtonEnabled(isPrev = false, isEnabled = false)
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
        fun newInstance() = DeviceAddGuideFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}