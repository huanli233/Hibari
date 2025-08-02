package com.huanli233.hibari.animation

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// This function exists so we do *not* inline the throw. It keeps
// the call site much smaller and since it's the slow path anyway,
// we don't mind the extra function call
internal fun throwIllegalArgumentException(message: String) {
    throw IllegalArgumentException(message)
}

// Like Kotlin's require() but without the .toString() call
@Suppress("BanInlineOptIn") // same opt-in as using Kotlin's require()
@OptIn(ExperimentalContracts::class)
internal inline fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalArgumentException(lazyMessage())
    }
}

// See above
internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

internal fun throwIllegalStateExceptionForNullCheck(message: String): Nothing {
    throw IllegalStateException(message)
}

// Like Kotlin's check() but without the .toString() call
@Suppress("BanInlineOptIn") // same opt-in as using Kotlin's check()
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalStateException(lazyMessage())
    }
}

// Like Kotlin's checkNotNull() but without the .toString() call
@Suppress("BanInlineOptIn") // same opt-in as using Kotlin's check()
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> checkPreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalStateExceptionForNullCheck(lazyMessage())
    }
    return value
}