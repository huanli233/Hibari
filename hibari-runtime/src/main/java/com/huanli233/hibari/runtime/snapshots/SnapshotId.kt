/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package com.huanli233.hibari.runtime.snapshots

import androidx.collection.mutableLongListOf

typealias SnapshotId = Long

internal const val SnapshotIdZero: SnapshotId = 0L
internal const val SnapshotIdMax: SnapshotId = Long.MAX_VALUE
internal const val SnapshotIdSize: Int = Long.SIZE_BITS
internal const val SnapshotIdInvalidValue: SnapshotId = -1

internal inline operator fun SnapshotId.compareTo(other: SnapshotId): Int =
    this.compareTo(other)

internal inline operator fun SnapshotId.compareTo(other: Int): Int =
    this.compareTo(other.toLong())

internal inline operator fun SnapshotId.plus(other: Int): SnapshotId = this + other.toLong()

internal inline operator fun SnapshotId.minus(other: SnapshotId): SnapshotId = this - other

internal inline operator fun SnapshotId.minus(other: Int): SnapshotId = this - other.toLong()

internal inline operator fun SnapshotId.div(other: Int): SnapshotId = this / other.toLong()

internal inline operator fun SnapshotId.times(other: Int): SnapshotId = this * other.toLong()

inline fun SnapshotId.toInt(): Int = this.toInt()

inline fun SnapshotId.toLong(): Long = this

typealias SnapshotIdArray = LongArray

internal fun snapshotIdArrayWithCapacity(capacity: Int): SnapshotIdArray =
    LongArray(capacity)

internal inline operator fun SnapshotIdArray.get(index: Int): SnapshotId = this[index]

internal inline operator fun SnapshotIdArray.set(index: Int, value: SnapshotId) {
    this[index] = value
}

internal inline val SnapshotIdArray.size: Int
    get() = this.size

internal inline fun SnapshotIdArray.copyInto(other: SnapshotIdArray) {
    this.copyInto(other, 0)
}

internal inline fun SnapshotIdArray.first(): SnapshotId = this[0]

internal fun SnapshotIdArray.binarySearch(id: SnapshotId): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1)
        val midVal = get(mid)
        if (id > midVal) low = mid + 1 else if (id < midVal) high = mid - 1 else return mid
    }
    return -(low + 1)
}

internal inline fun SnapshotIdArray.forEach(block: (SnapshotId) -> Unit) {
    for (value in this) {
        block(value)
    }
}

internal fun SnapshotIdArray.withIdInsertedAt(index: Int, id: SnapshotId): SnapshotIdArray {
    val newSize = size + 1
    val newArray = LongArray(newSize)
    this.copyInto(destination = newArray, destinationOffset = 0, startIndex = 0, endIndex = index)
    this.copyInto(
        destination = newArray,
        destinationOffset = index + 1,
        startIndex = index,
        endIndex = newSize - 1,
    )
    newArray[index] = id
    return newArray
}

internal fun SnapshotIdArray.withIdRemovedAt(index: Int): SnapshotIdArray? {
    val newSize = this.size - 1
    if (newSize == 0) {
        return null
    }
    val newArray = LongArray(newSize)
    if (index > 0) {
        this.copyInto(
            destination = newArray,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = index,
        )
    }
    if (index < newSize) {
        this.copyInto(
            destination = newArray,
            destinationOffset = index,
            startIndex = index + 1,
            endIndex = newSize + 1,
        )
    }
    return newArray
}

internal class SnapshotIdArrayBuilder(array: SnapshotIdArray?) {
    private val list = array?.let { mutableLongListOf(*array) } ?: mutableLongListOf()

    fun add(id: SnapshotId) {
        list.add(id)
    }

    fun toArray(): SnapshotIdArray? {
        val size = list.size
        if (size == 0) return null
        val result = LongArray(size)
        list.forEachIndexed { index, element -> result[index] = element }
        return result
    }
}

internal inline fun snapshotIdArrayOf(id: SnapshotId): SnapshotIdArray = longArrayOf(id)

internal fun Int.toSnapshotId(): SnapshotId = toLong()

internal fun Long.toSnapshotId(): SnapshotId = this