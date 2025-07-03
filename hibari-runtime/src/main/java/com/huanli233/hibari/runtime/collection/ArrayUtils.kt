package com.huanli233.hibari.runtime.collection

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> Array<out T>.fastCopyInto(
    destination: Array<T>,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int,
): Array<T> {
    System.arraycopy(this, startIndex, destination, destinationOffset, endIndex - startIndex)
    return destination
}