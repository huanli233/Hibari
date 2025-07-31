@file:Suppress("NOTHING_TO_INLINE")

package com.huanli233.hibari.ui.util

import kotlin.math.max
import kotlin.math.pow

/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// These two functions are technically identical to Float.fromBits()
// and Double.fromBits(). However, since they are declared as top-
// level functions, they do not incur the cost of a static fetch
// through the Companion class. Using these top-level functions,
// the generated arm64 code after dex2oat is exactly a single `fmov`

inline fun floatFromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)

inline fun doubleFromBits(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

inline fun Float.fastRoundToInt(): Int = Math.round(this)

inline fun Double.fastRoundToInt(): Int = Math.round(this).toInt()

/**
 * Packs two Float values into one Long value for use in inline classes.
 */
inline fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toRawBits().toLong()
    val v2 = val2.toRawBits().toLong()
    return (v1 shl 32) or (v2 and 0xFFFFFFFF)
}

/**
 * Unpacks the first Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat1(value: Long): Float {
    return floatFromBits((value shr 32).toInt())
}

/**
 * Unpacks the first absolute Float value in [packFloats] from its returned Long.
 */
inline fun unpackAbsFloat1(value: Long): Float {
    return floatFromBits(((value shr 32) and 0x7FFFFFFF).toInt())
}

/**
 * Unpacks the second Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat2(value: Long): Float {
    return floatFromBits((value and 0xFFFFFFFF).toInt())
}

/**
 * Unpacks the second absolute Float value in [packFloats] from its returned Long.
 */
inline fun unpackAbsFloat2(value: Long): Float {
    return floatFromBits((value and 0x7FFFFFFF).toInt())
}

/**
 * Packs two Int values into one Long value for use in inline classes.
 */
inline fun packInts(val1: Int, val2: Int): Long {
    return (val1.toLong() shl 32) or (val2.toLong() and 0xFFFFFFFF)
}

/**
 * Unpacks the first Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt1(value: Long): Int {
    return (value shr 32).toInt()
}

/**
 * Unpacks the second Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt2(value: Long): Int {
    return (value and 0xFFFFFFFF).toInt()
}

// Masks everything but the sign bit
internal const val DualUnsignedFloatMask = 0x7fffffff_7fffffffL

// Any value greater than this is a NaN
internal const val FloatInfinityBase = 0x7f800000L
internal const val DualFloatInfinityBase = 0x7f800000_7f800000L

// Same as Offset/Size.Unspecified.packedValue, but avoids a getstatic
internal const val UnspecifiedPackedFloats = 0x7fc00000_7fc00000L // NaN_NaN

// 0x80000000_80000000UL.toLong() but expressed as a const value
// Mask for the sign bit of the two floats packed in a long
internal const val DualFloatSignBit = -0x7fffffff_80000000L
// Set the highest bit of each 32 bit chunk in a 64 bit word
internal const val Uint64High32 = -0x7fffffff_80000000L
// Set the lowest bit of each 32 bit chunk in a 64 bit word
internal const val Uint64Low32 = 0x00000001_00000001L
// Encodes the first valid NaN in each of the 32 bit chunk of a 64 bit word
internal const val DualFirstNaN = 0x7f800001_7f800001L

internal fun Float.toStringAsFixed(digits: Int): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this < 0f) "-Infinity" else "Infinity"

    val clampedDigits: Int = max(digits, 0) // Accept positive numbers and 0 only
    val pow = 10f.pow(clampedDigits)
    val shifted = this * pow // shift the given value by the corresponding power of 10
    val decimal = shifted - shifted.toInt() // obtain the decimal of the shifted value
    // Manually round up if the decimal value is greater than or equal to 0.5f.
    // because kotlin.math.round(0.5f) rounds down
    val roundedShifted = if (decimal >= 0.5f) {
        shifted.toInt() + 1
    } else {
        shifted.toInt()
    }

    val rounded = roundedShifted / pow // divide off the corresponding power of 10 to shift back
    return if (clampedDigits > 0) {
        // If we have any decimal points, convert the float to a string
        rounded.toString()
    } else {
        // If we do not have any decimal points, return the int
        // based string representation
        rounded.toInt().toString()
    }
}
