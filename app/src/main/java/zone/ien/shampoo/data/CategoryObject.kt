package zone.ien.shampoo.data

import android.graphics.drawable.Drawable

data class CategoryObject(
    val id: Int,
    val icon: Drawable?,
    val title: String,
    val content: String
)