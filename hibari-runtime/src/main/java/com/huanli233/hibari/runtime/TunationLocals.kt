package com.huanli233.hibari.runtime

import android.content.Context

val LocalContext = staticTunationLocalOf<Context> {
    noLocalProvidedFor("LocalContext")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("TunationLocal $name not present")
}