package zone.ien.shampoo.utils

import android.content.res.Resources
import android.util.TypedValue
import com.google.android.material.R

object ColorUtils {
    private fun getAttrColor(theme: Resources.Theme, id: Int): Int = TypedValue().apply { theme.resolveAttribute(id, this, true) }.data

    fun getAttrColor(theme: Resources.Theme, resourceName: Colors): Int {
        val id = MyUtils.getResourceId(resourceName.name, R.attr::class.java)
        return getAttrColor(theme, id)
    }
}

enum class Colors(name: String) {
    colorContainer("colorContainer"),
    colorOnContainer("colorOnContainer"),
    colorOnContainerUnchecked("colorOnContainerUnchecked"),

    colorError("colorError"),
    colorErrorContainer("colorErrorContainer"),
    colorOnError("colorOnError"),
    colorOnErrorContainer("colorOnErrorContainer"),

    colorPrimary("colorPrimary"),
    colorPrimaryContainer("colorPrimaryContainer"),
    colorPrimaryFixed("colorPrimaryFixed"),
    colorPrimaryFixedDim("colorPrimaryFixedDim"),
    colorPrimaryInverse("colorPrimaryInverse"),
    colorPrimarySurface("colorPrimarySurface"),
    colorPrimaryVariant("colorPrimaryVariant"),

    colorOnPrimary("colorOnPrimary"),
    colorOnPrimaryContainer("colorOnPrimaryContainer"),
    colorOnPrimaryFixedVariant("colorOnPrimaryFixedVariant"),
    colorOnPrimarySurface("colorOnPrimarySurface"),

    colorSecondary("colorSecondary"),
    colorSecondaryContainer("colorSecondaryContainer"),
    colorSecondaryFixed("colorSecondaryFixed"),
    colorSecondaryFixedDim("colorSecondaryFixedDim"),
    colorSecondaryVariant("colorSecondaryVariant"),

    colorOnSecondary("colorOnSecondary"),
    colorOnSecondaryContainer("colorOnSecondaryContainer"),
    colorOnSecondaryFixed("colorOnSecondaryFixed"),
    colorOnSecondaryFixedVariant("colorOnSecondaryFixedVariant"),

    colorTertiary("colorTertiary"),
    colorTertiaryContainer("colorTertiaryContainer"),
    colorTertiaryFixed("colorTertiaryFixed"),
    colorTertiaryFixedDim("colorTertiaryFixedDim"),

    colorOnTertiary("colorOnTertiary"),
    colorOnTertiaryContainer("colorOnTertiaryContainer"),
    colorOnTertiaryFixed("colorOnTertiaryFixed"),
    colorOnTertiaryFixedVariant("colorOnTertiaryFixedVariant"),

    colorSurface("colorSurface"),
    colorSurfaceBright("colorSurfaceBright"),
    colorSurfaceContainer("colorSurfaceContainer"),
    colorSurfaceContainerHigh("colorSurfaceContainerHigh"),
    colorSurfaceContainerHighest("colorSurfaceContainerHighest"),
    colorSurfaceContainerLow("colorSurfaceContainerLow"),
    colorSurfaceContainerLowest("colorSurfaceContainerLowest"),
    colorSurfaceDim("colorSurfaceDim"),
    colorSurfaceInverse("colorSurfaceInverse"),
    colorSurfaceVariant("colorSurfaceVariant"),

    colorOnSurface("colorOnSurface"),
    colorOnSurfaceInverse("colorOnSurfaceInverse"),
    colorOnSurfaceVariant("colorOnSurfaceVariant"),

    colorOutline("colorOutline"),
    colorOutlineVariant("colorOutlineVariant"),
}