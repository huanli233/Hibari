package com.huanli233.hibari.ui.unit

import android.content.Context
import com.huanli233.hibari.ui.unit.fontscaling.FontScaleConverter
import com.huanli233.hibari.ui.unit.fontscaling.FontScaleConverterFactory

/**
 * Creates a [Density] for this [Context].
 *
 * @param context density values will be extracted from this [Context]
 */
fun Density(context: Context): Density {
    val fontScale = context.resources.configuration.fontScale
    return DensityWithConverter(
        context.resources.displayMetrics.density,
        fontScale,
        FontScaleConverterFactory.forScale(fontScale) ?: LinearFontScaleConverter(fontScale)
    )
}

private data class DensityWithConverter(
    override val density: Float,
    override val fontScale: Float,
    private val converter: FontScaleConverter
) : Density {

    override fun Dp.toSp(): TextUnit {
        return converter.convertDpToSp(value).sp
    }

    override fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(converter.convertSpToDp(value))
    }
}

private data class LinearFontScaleConverter(private val fontScale: Float) : FontScaleConverter {
    override fun convertSpToDp(sp: Float) = sp * fontScale
    override fun convertDpToSp(dp: Float) = dp / fontScale
}
