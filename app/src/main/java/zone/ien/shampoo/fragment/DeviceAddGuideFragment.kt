package zone.ien.shampoo.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zone.ien.shampoo.BuildConfig
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.FragmentDeviceAddGuideBinding
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DeviceAddGuideFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddGuideBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null

    private var determinedValue = 0
    private var determinedTime = System.currentTimeMillis()
    private var zeroDeterminedValue = 0
    private var zeroDeterminedTime = System.currentTimeMillis()
    private var zeroThresholdTime = 5 * 1000
    private var thresholdTime = 10 * 1000
    private var isMeasuring = false
    private var isZeroing = true
    private var zero = -1
    private var zeroValues = arrayListOf<Int>()
    private var values = arrayListOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_guide, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgGuide.clipToOutline = true

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val type = intent.getIntExtra(IntentKey.MESSAGE_TYPE, -1)
                val value = intent.getIntExtra(IntentKey.MESSAGE_VALUE, -1)
                Dlog.d(TAG, "receive $value $zeroDeterminedValue")

                if (type == MessageType.TYPE_MEASURE_INIT) {
                    if (isZeroing) {
                        zeroValues.add(value)
                        if (zeroDeterminedValue != 0) {
                            if (abs(zeroDeterminedValue - value) > 40) {
                                zeroDeterminedValue = 0
                                zeroValues.clear()
                            } else if (System.currentTimeMillis() - zeroDeterminedTime >= zeroThresholdTime) {
                                isZeroing = false
                                isMeasuring = true
                                zero = zeroValues.average().toInt()

                                binding.tvState.text = getString(R.string.measuring)

                                // 소리내기
                                val mediaPlayer = MediaPlayer()
                                val fileDescriptor = resources.openRawResourceFd(R.raw.zeroing_end)
                                mediaPlayer.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
                                fileDescriptor.close()
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                            }
                        } else {
                            zeroDeterminedValue = value
                            zeroDeterminedTime = System.currentTimeMillis()
                            zeroValues.clear()
                        }
                    } else if (isMeasuring) {
                        val valueWithZero = abs(value - zero)
                        binding.tvWeight.text = valueWithZero.toString()
                        values.add(valueWithZero)
                        Dlog.d(TAG, "receive ${Date(determinedTime)} ${determinedValue} ${valueWithZero}")
                        if (determinedValue != 0) { // 어쨌든 값이 정해졌음 - 10초 동안 determinedValue가 유지되면 그 값으로 결정.
                            if (abs(determinedValue - valueWithZero) > 40) {
                                determinedValue = 0
                                values.clear()
                            } else if (System.currentTimeMillis() - determinedTime >= thresholdTime) { // 값 결정
                                isMeasuring = false
                                binding.btnMeasureAgain.isEnabled = true
                                binding.progress.visibility = View.INVISIBLE
                                binding.icCheck.visibility = View.VISIBLE
                                binding.tvState.text = binding.tvState.context.getString(R.string.measurement_completed)
                                binding.tvWeight.text = values.average().toString()
                                DeviceAddActivity.deviceCapacity = values.average().toInt()
                                callbackListener?.setButtonEnabled(isPrev = false, isEnabled = true)
                            }
                        } else {
                            determinedValue = valueWithZero
                            determinedTime = System.currentTimeMillis()
                            values.clear()
                        }
                    }
                }


            }
        }, IntentFilter(IntentID.CALLBACK_DEVICE_VALUE), Context.RECEIVER_EXPORTED)

        binding.btnMeasureAgain.setOnClickListener {
            isMeasuring = true
            binding.btnMeasureAgain.isEnabled = false
            binding.progress.visibility = View.VISIBLE
            binding.icCheck.visibility = View.INVISIBLE
            binding.tvState.text = getString(R.string.measuring)
            determinedTime = System.currentTimeMillis()
            values.clear()
            enableMeasure()

            callbackListener?.setButtonEnabled(isPrev = false, isEnabled = false)
        }
        binding.btnMeasureAgain.isEnabled = false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun enableMeasure() {
        GlobalScope.launch(Dispatchers.IO) {
            val unit = 200L
            while (true) {
                requireContext().sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                    putExtra(IntentKey.DEVICE_ADDRESS, DeviceAddActivity.deviceAddress)
                    putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_MEASURE_INIT)
                    putExtra(IntentKey.MESSAGE_VALUE, "request")
                })
                delay(unit)

                if (!isZeroing && !isMeasuring) break
            }
        }
    }

    fun setCallbackListener(callbackListener: DeviceAddActivityCallback?) {
        this.callbackListener = callbackListener
    }

    override fun onResume() {
        super.onResume()
        callbackListener?.setButtonEnabled(isPrev = true, isEnabled = true)
        callbackListener?.setButtonEnabled(isPrev = false, isEnabled = false)
        callbackListener?.setTitle(R.string.measure_current_capacity)

        isZeroing = true
        isMeasuring = false
        enableMeasure()
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