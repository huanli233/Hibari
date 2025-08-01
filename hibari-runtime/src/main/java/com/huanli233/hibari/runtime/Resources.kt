package com.huanli233.hibari.runtime

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.highcapable.betterandroid.ui.extension.component.base.getColorCompat
import com.highcapable.betterandroid.ui.extension.component.base.getColorStateListCompat
import com.highcapable.betterandroid.ui.extension.component.base.getDrawableCompat
import com.highcapable.betterandroid.ui.extension.component.base.getFontCompat
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsBoolean
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsDimension
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsDrawable
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsFloat
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsId
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsString
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsStringArray
import com.huanli233.hibari.runtime.resources.MaterialAttributes

@Tunable
fun stringRes(@StringRes resId: Int, vararg formatArgs: Any) =
    if (formatArgs.isNotEmpty())
        currentContext.getString(resId, *formatArgs)
    else currentContext.getString(resId)
@Tunable
fun colorRes(@ColorRes resId: Int) = currentContext.getColorCompat(resId)
@Tunable
fun stateColorRes(@ColorRes resId: Int) = currentContext.getColorStateListCompat(resId)
@Tunable
fun drawableRes(@DrawableRes resId: Int) = currentContext.getDrawableCompat(resId)
@Tunable
fun bitmapRes(@DrawableRes resId: Int) = currentContext.getDrawableCompat(resId).toBitmap()
@Tunable
fun dimenRes(@DimenRes resId: Int) = currentContext.resources.getDimension(resId)
@Tunable
fun fontRes(@FontRes resId: Int) = currentContext.getFontCompat(resId)

@Tunable
fun attrColorRes(@AttrRes resId: Int) = attrColorResOrNull(resId) ?: throw IllegalArgumentException("The attribute $resId is not present in the current theme.")
@Tunable
fun attrColorResOr(@AttrRes resId: Int, defaultValue: Int) = attrColorResOrNull(resId) ?: defaultValue
@Tunable
fun attrColorResOrNull(@AttrRes resId: Int) = resolveColor(currentContext,
    MaterialAttributes.resolveTypedValueOrThrow(currentContext, resId))
@Tunable
fun attrColorStateListRes(@AttrRes resId: Int) = attrColorStateListResOrNull(resId) ?: throw IllegalArgumentException("The attribute $resId is not present in the current theme.")
@Tunable
fun attrColorStateListResOr(@AttrRes resId: Int, defaultValue: ColorStateList) = attrColorStateListResOrNull(resId) ?: defaultValue
@Tunable
fun attrColorStateListResOrNull(@AttrRes resId: Int) =
    MaterialAttributes.resolve(currentContext, resId)?.let {
        resolveColorStateList(currentContext,
            it)
    }

private fun resolveColor(context: Context, typedValue: TypedValue): Int? {
    return if (typedValue.resourceId != 0) {
        ContextCompat.getColor(context, typedValue.resourceId)
    } else {
        typedValue.data
    }
}
private fun resolveColorStateList(
    context: Context, typedValue: TypedValue
): ColorStateList? {
    return if (typedValue.resourceId != 0) {
        ContextCompat.getColorStateList(context, typedValue.resourceId)
    } else {
        ColorStateList.valueOf(typedValue.data)
    }
}

@Tunable
fun attrDrawableRes(@AttrRes resId: Int) = currentContext.getThemeAttrsDrawable(resId)
@Tunable
fun attrDimenRes(@AttrRes resId: Int) = currentContext.getThemeAttrsDimension(resId)
@Tunable
fun attrStringRes(@AttrRes resId: Int) = currentContext.getThemeAttrsString(resId)
@Tunable
fun attrStringArrayRes(@AttrRes resId: Int) = currentContext.getThemeAttrsStringArray(resId)
@Tunable
fun attrIdRes(@AttrRes resId: Int) = currentContext.getThemeAttrsId(resId)