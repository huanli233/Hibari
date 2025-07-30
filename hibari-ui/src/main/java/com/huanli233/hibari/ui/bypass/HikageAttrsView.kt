package com.huanli233.hibari.ui.bypass

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep

/**
 * Just a view for obtaining [AttributeSet].
 *
 * **DONT USE THIS VIEW IN YOUR LAYOUT.**
 */
@Keep
class HibariAttrsView internal constructor(context: Context, internal val attrs: AttributeSet?) : View(context, attrs)