package com.huanli233.hibari.runtime

import com.huanli233.hibari.ui.Modifier

data class IdAttribute(val id: String) : Modifier.Element
fun Modifier.id(id: String): Modifier {
    return this.then(IdAttribute(id))
}