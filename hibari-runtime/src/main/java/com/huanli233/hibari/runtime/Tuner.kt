package com.huanli233.hibari.runtime

internal class HibariRuntimeError(override val message: String) : IllegalStateException()

internal fun hibariRuntimeError(message: String): Nothing {
    throw HibariRuntimeError(
        "Hibari Runtime internal error. Unexpected or incorrect use of the Hibari " +
                "internal runtime API ($message)."
    )
}