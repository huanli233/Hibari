package com.huanli233.hibari.runtime.platform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal typealias SynchronizedObject = Any

@Suppress("NOTHING_TO_INLINE")
internal inline fun makeSynchronizedObject(ref: Any? = null) = ref ?: SynchronizedObject()

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class, ExperimentalContracts::class)
@PublishedApi
internal inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return kotlin.synchronized(lock, block)
}