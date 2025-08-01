/*
 * Copyright 2020 The Android Open Source Project
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

package com.huanli233.hibari.ui.layout

import com.huanli233.hibari.ui.layout.ContentScale.Companion.None
import com.huanli233.hibari.ui.unit.Size
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a rule to apply to scale a source rectangle to be inscribed into a destination
 */
interface ContentScale {

    /**
     * Computes the scale factor to apply to the horizontal and vertical axes independently
     * of one another to fit the source appropriately with the given destination
     */
    fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor

    /**
     * Companion object containing commonly used [ContentScale] implementations
     */
    companion object {

        /**
         * Scale the source uniformly (maintaining the source's aspect ratio) so that both
         * dimensions (width and height) of the source will be equal to or larger than the
         * corresponding dimension of the destination.
         *
         * This [ContentScale] implementation in combination with usage of [Alignment.Center]
         * provides similar behavior to [android.widget.ImageView.ScaleType.CENTER_CROP]
         */
        val Crop = object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
                computeFillMaxDimension(srcSize, dstSize).let {
                    ScaleFactor(it, it)
                }
        }

        /**
         * Scale the source uniformly (maintaining the source's aspect ratio) so that both
         * dimensions (width and height) of the source will be equal to or less than the
         * corresponding dimension of the destination
         *
         * This [ContentScale] implementation in combination with usage of [Alignment.Center]
         * provides similar behavior to [android.widget.ImageView.ScaleType.FIT_CENTER]
         */
        val Fit = object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
                computeFillMinDimension(srcSize, dstSize).let {
                    ScaleFactor(it, it)
                }
        }

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the destination
         * height. This can cover a larger area than the destination if the height is larger than
         * the width.
         */
        val FillHeight = object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
                computeFillHeight(srcSize, dstSize).let {
                    ScaleFactor(it, it)
                }
        }

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the
         * destination width. This can cover a larger area than the destination if the width is
         * larger than the height.
         */
        val FillWidth = object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
                computeFillWidth(srcSize, dstSize).let {
                    ScaleFactor(it, it)
                }
        }

        /**
         * Scale the source to maintain the aspect ratio to be inside the destination bounds
         * if the source is larger than the destination. If the source is smaller than or equal
         * to the destination in both dimensions, this behaves similarly to [None]. This will
         * always be contained within the bounds of the destination.
         *
         * This [ContentScale] implementation in combination with usage of [Alignment.Center]
         * provides similar behavior to [android.widget.ImageView.ScaleType.CENTER_INSIDE]
         */
        val Inside = object : ContentScale {

            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
                return if (srcSize.width <= dstSize.width &&
                    srcSize.height <= dstSize.height
                ) {
                    ScaleFactor(1.0f, 1.0f)
                } else {
                    computeFillMinDimension(srcSize, dstSize).let {
                        ScaleFactor(it, it)
                    }
                }
            }
        }

        /**
         * Do not apply any scaling to the source
         */
        val None = FixedScale(1.0f)

        /**
         * Scale horizontal and vertically non-uniformly to fill the destination bounds.
         */
        val FillBounds = object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
                ScaleFactor(
                    computeFillWidth(srcSize, dstSize),
                    computeFillHeight(srcSize, dstSize)
                )
        }
    }
}

/**
 * [ContentScale] implementation that always scales the dimension by the provided
 * fixed floating point value
 */
data class FixedScale(val value: Float) : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
        ScaleFactor(value, value)
}

private fun computeFillMaxDimension(srcSize: Size, dstSize: Size): Float {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return max(widthScale, heightScale)
}

private fun computeFillMinDimension(srcSize: Size, dstSize: Size): Float {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return min(widthScale, heightScale)
}

private fun computeFillWidth(srcSize: Size, dstSize: Size): Float =
    dstSize.width / srcSize.width

private fun computeFillHeight(srcSize: Size, dstSize: Size): Float =
    dstSize.height / srcSize.height
