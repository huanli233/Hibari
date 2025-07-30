@file:Suppress("NOTHING_TO_INLINE")

package com.huanli233.hibari.ui.util

inline fun floatFromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)

inline fun doubleFromBits(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

inline fun Float.fastRoundToInt(): Int = Math.round(this)

inline fun Double.fastRoundToInt(): Int = Math.round(this).toInt()