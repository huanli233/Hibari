package com.huanli233.hibari.ui

interface Modifier {
    /**
     * Chains this modifier with another one.
     */
    fun then(other: Modifier): Modifier

    /**
     * Folds over the elements of this modifier.
     * `initial` is the starting value, and `operation` is applied to each element.
     */
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    // An element of a Modifier chain. Attribute is a specialized Element.
    interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = operation(initial, this)
        override fun then(other: Modifier): Modifier =
            if (other === Modifier) this else CombinedModifier(this, other)
    }

    /**
     * The companion object serves as the entry point for creating modifiers.
     * It is an empty modifier by itself.
     */
    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun then(other: Modifier): Modifier = other
    }
}

private class CombinedModifier(
    private val outer: Modifier,
    private val inner: Modifier
) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)
}

fun Modifier.flattenToList(): List<Modifier.Element> {
    return this.foldIn(mutableListOf()) { acc, element ->
        acc += element
        acc
    }
}

fun List<Modifier.Element>.viewAttributes(): List<ViewAttribute<*, *>> {
    return this.mapNotNull {
        it as? ViewAttribute<*, *>
    }
}

fun List<Modifier.Element>.layoutAttributes(): List<LayoutAttribute<*, *>> {
    return this.mapNotNull {
        it as? LayoutAttribute<*, *>
    }
}