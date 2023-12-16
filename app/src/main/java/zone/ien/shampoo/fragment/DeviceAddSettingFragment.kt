package zone.ien.shampoo.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.opencsv.CSVReader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.adapter.EmojiMoreAdapter
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.callback.EmojiClickCallback
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.IntentValue
import zone.ien.shampoo.databinding.DialogEmojiMoreBinding
import zone.ien.shampoo.databinding.DialogInputBinding
import zone.ien.shampoo.databinding.FragmentDeviceAddGuideBinding
import zone.ien.shampoo.databinding.FragmentDeviceAddInfoBinding
import zone.ien.shampoo.databinding.FragmentDeviceAddSettingBinding
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.room.PlaceDatabase
import zone.ien.shampoo.room.PlaceEntity
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.emoji_labels
import zone.ien.shampoo.utils.MyUtils.emojis
import java.io.InputStreamReader

class DeviceAddSettingFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddSettingBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null
    private var productList: ArrayList<Array<String>> = arrayListOf()

    private var placeDatabase: PlaceDatabase? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_setting, container, false)

//        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placeDatabase = PlaceDatabase.getInstance(requireContext())

        val assetManager = requireActivity().assets
        val inputStream = assetManager.open("bubble_datasheet.csv")
        val csvReader = CSVReader(InputStreamReader(inputStream, "EUC-KR"))

        binding.imgPreview.clipToOutline = true

        productList.addAll(csvReader.readAll())

        binding.inputName.editText?.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                callbackListener?.setFinishButtonEnabled(p0.toString() != "" && binding.autoRoom.text.toString() != "")
                if (p0.toString() != "") {
                    DeviceAddActivity.deviceName = p0.toString()
                }
            }
        })

        GlobalScope.launch(Dispatchers.IO) {
            val places = placeDatabase?.getDao()?.getAll() as ArrayList<PlaceEntity>
            places.add(PlaceEntity("➕", getString(R.string.add_new_place)))

            withContext(Dispatchers.Main) {
                val items = places.map { "${it.icon}  ${it.title}" } as ArrayList
                val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)

                binding.autoRoom.setAdapter(arrayAdapter)
                binding.autoRoom.setOnItemClickListener { parent, v, position, id ->
                    if (position == items.lastIndex) {
                        val dialogBinding: DialogInputBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_input, null, false)
                        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                            dialogBinding.inputLayout.hint = context.getString(R.string.new_place)
                            dialogBinding.tvEmojiPreview.text = "🛁"

                            setPositiveButton(android.R.string.ok) { dialog, id -> }
                            setNegativeButton(android.R.string.cancel) { dialog, id ->
                                binding.autoRoom.setText("", false)
                            }

                            setView(dialogBinding.root)
                        }.create()

                        dialogBinding.tvEmojiPreview.setOnClickListener {
                            var dialog2: AlertDialog? = null
                            val callbackListener = object: EmojiClickCallback {
                                override fun click(emoji: String) {
                                    dialogBinding.tvEmojiPreview.text = emoji
                                    dialog2?.dismiss()
                                }
                            }
                            dialog2 = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                                val emojiBinding: DialogEmojiMoreBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_emoji_more, LinearLayout(requireContext()), false)
                                for (i in 0 until emojiBinding.tabLayout.tabCount) emojiBinding.tabLayout.getTabAt(i)?.text = emoji_labels[i]

                                val gridLayoutManager = GridLayoutManager(requireContext(), MyUtils.calculateNoOfColumns(requireContext(), 48f))
                                emojiBinding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                                    override fun onTabSelected(tab: TabLayout.Tab) {
                                        emojiBinding.list.adapter = EmojiMoreAdapter(emojis[tab.position]).apply {
                                            setCallbackListener(callbackListener)
                                        }
                                    }
                                    override fun onTabReselected(tab: TabLayout.Tab) {}
                                    override fun onTabUnselected(tab: TabLayout.Tab) {}
                                })
                                emojiBinding.list.layoutManager = gridLayoutManager
                                emojiBinding.list.adapter = EmojiMoreAdapter(emojis[0]).apply {
                                    setCallbackListener(callbackListener)
                                }
                                emojiBinding.tabLayout.selectTab(emojiBinding.tabLayout.getTabAt(0))

                                setNegativeButton(android.R.string.cancel) { _, _ -> }

                                setView(emojiBinding.root)
                            }.create()
                            dialog2.show()
                        }

                        dialog.show()
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val title = dialogBinding.inputLayout.editText?.text?.toString() ?: ""
                            val icon = dialogBinding.tvEmojiPreview.text.toString()
                            if (title != "") {
                                GlobalScope.launch(Dispatchers.IO) {
                                    val id = placeDatabase?.getDao()?.add(PlaceEntity(icon, title))
                                    withContext(Dispatchers.Main) {
                                        val entity = PlaceEntity(icon, title).apply { this.id = id }
                                        places.add(entity)
                                        arrayAdapter.insert("${icon}  ${title}", items.lastIndex)
                                        arrayAdapter.notifyDataSetChanged()
                                        binding.autoRoom.setText("${icon}  ${title}", false)
                                        callbackListener?.setFinishButtonEnabled(binding.inputName.editText?.text?.toString() != "" && binding.autoRoom.text.toString() != "")

                                        DeviceAddActivity.deviceRoom = entity.id ?: -1
                                        dialog.dismiss()
                                    }
                                }
                            } else {
                                dialog.window?.decorView?.animate()?.translationX(16f)?.interpolator = CycleInterpolator(7f)
                                dialogBinding.inputLayout.error = getString(R.string.fill_out_the_title)
                            }
                        }


                    } else {
                        callbackListener?.setFinishButtonEnabled(binding.inputName.editText?.text?.toString() != "" && binding.autoRoom.text.toString() != "")
                        DeviceAddActivity.deviceRoom = places[position].id ?: -1
                    }
                }
            }
        }

    }

    fun setCallbackListener(callbackListener: DeviceAddActivityCallback?) {
        this.callbackListener = callbackListener
    }

    override fun onResume() {
        super.onResume()
        Dlog.d(TAG, "${DeviceAddActivity.deviceBarcode}")
        val product = productList.find { it[1].replace(" ", "") == DeviceAddActivity.deviceBarcode }
        callbackListener?.setButtonEnabled(isPrev = true, isEnabled = false)
        callbackListener?.setFinishButtonShow(true)
        callbackListener?.setFinishButtonEnabled(false)
        callbackListener?.setTitle(R.string.finish_setting)

        product?.let {
            Dlog.d(TAG, it.toString())
            binding.tvLiquidMax.text = getString(R.string.capacity_max_format, it[4].toInt())
            binding.tvLiquidCurrent.text = getString(R.string.capacity_current_format, DeviceAddActivity.deviceCapacity)
            binding.tvContentModel.text = it[2]
            binding.tvContentType.text = DeviceEntity.getTypeString(requireContext(), it[3].toInt())
            binding.tvContentModel.isSelected = true

            binding.progressBar.max = it[4].toInt()
            binding.progressBar.progress = DeviceAddActivity.deviceCapacity

            if (it[7] != "") {
                Dlog.d(TAG, "link: ${it[7]}")
                Glide.with(this)
                    .load(it[7])
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
        fun newInstance() = DeviceAddSettingFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}