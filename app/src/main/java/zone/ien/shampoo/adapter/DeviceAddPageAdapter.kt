package zone.ien.shampoo.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DeviceAddPageAdapter(activity: FragmentActivity, var pages: List<Fragment>): FragmentStateAdapter(activity) {

//    private var callbackListener: TimerFragmentCallback? = null

    override fun createFragment(position: Int): Fragment {
        return pages[position % pages.size]
    }

    override fun getItemCount(): Int = pages.size//Int.MAX_VALUE

    companion object {
        const val LOOP_COUNT = 1000
    }
}