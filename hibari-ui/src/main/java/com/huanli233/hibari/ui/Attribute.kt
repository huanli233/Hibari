package com.huanli233.hibari.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.XmlRes
import androidx.core.view.updateLayoutParams
import com.highcapable.yukireflection.type.android.ViewGroupClass

val uniqueKey: String
    get() { throw NotImplementedError("Implemented as an intrinsic") }

fun uniqueKeyForParam(param: Any): String {
    throw NotImplementedError("Implemented as an intrinsic")
}

data class AttributeKey<T>(val name: String)

interface AttributeApplier<in V, in T> {
    fun apply(target: V, value: T)
}

interface LayoutAttributeApplier<in LP, in T, in V> {
    fun apply(target: LP, value: T, view: V)
}

/**
 * An atomic configuration unit for a LayoutNode, which will be translated
 * into a View property or a specific setup action by the Renderer.
 */
interface Attribute<T> : Modifier.Element {
    val value: T
    /**
     * A unique key for this attribute type. Used during diffing to identify
     * if an attribute of the same type already exists.
     */
    val key: Any

    fun applyTo(view: View)

    /**
     * If true, the Renderer can update the View with the new attribute value
     * without recreating the View. If false, a change in this attribute
     * might force a View recreation depending on the Renderer's logic.
     */
    val reuseSupported: Boolean
}

data class ViewAttribute<V : View, T>(
    override val key: Any,
    val applier: AttributeApplier<V, T>,
    override val value: T,
    override val reuseSupported: Boolean = true
) : Attribute<T> {

    override fun applyTo(view: View) {
        @Suppress("UNCHECKED_CAST")
        applier.apply(view as V, value)
    }

    override fun hashCode(): Int {
        var result = reuseSupported.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + key.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewAttribute<*, *>

        if (reuseSupported != other.reuseSupported) return false
        if (value != other.value) return false
        if (key != other.key) return false

        return true
    }


}

data class LayoutAttribute<LP : ViewGroup.LayoutParams, T>(
    override val key: Any,
    val applier: LayoutAttributeApplier<LP, T, View>,
    override val value: T,
    override val reuseSupported: Boolean = true
) : Attribute<T> {

    override fun applyTo(view: View) {
        view.updateLayoutParams {
            @Suppress("UNCHECKED_CAST")
            applier.apply(this as LP, value, view)
        }
    }

    override fun hashCode(): Int {
        var result = reuseSupported.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + key.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LayoutAttribute<*, *>

        if (reuseSupported != other.reuseSupported) return false
        if (value != other.value) return false
        if (key != other.key) return false

        return true
    }
}

data class ViewClassAttribute(val viewClass: Class<out View>) : Modifier.Element
fun Modifier.viewClass(clazz: Class<out View>): Modifier {
    return this.then(ViewClassAttribute(clazz))
}

data class AttrsAttribute(val attrs: Int) : Modifier.Element
fun Modifier.attrs(@XmlRes attrs: Int): Modifier {
    return this.then(AttrsAttribute(attrs))
}

fun <V : View, T> Modifier.thenViewAttribute(key: Any, value: T, reuseSupported: Boolean = true, applier: V.(T) -> Unit): Modifier {
    return this.then(
        ViewAttribute(key, object : AttributeApplier<V, T> {
            override fun apply(target: V, value: T) {
                applier(target, value)
            }
        }, value)
    )
}

fun <V : View, T> Modifier.thenViewAttributeIfNotNull(key: Any, value: T?, reuseSupported: Boolean = true, block: V.(T) -> Unit): Modifier {
    return if (value != null) {
        this.thenViewAttribute(key, value, applier = block)
    } else {
        this
    }
}

fun <V : View> Modifier.thenUnitViewAttribute(key: Any, reuseSupported: Boolean = true, applier: V.() -> Unit): Modifier {
    return this.then(
        ViewAttribute(key, object : AttributeApplier<V, Unit> {
            override fun apply(target: V, value: Unit) {
                applier(target)
            }
        }, Unit)
    )
}

fun <V : ViewGroup.LayoutParams, T> Modifier.thenLayoutAttribute(key: Any, value: T, reuseSupported: Boolean = true, applier: V.(T, View) -> Unit): Modifier {
    return this.then(
        LayoutAttribute(key, object : LayoutAttributeApplier<V, T, View> {
            override fun apply(target: V, value: T, view: View) {
                applier(target, value, view)
            }
        }, value)
    )
}

fun <LP : ViewGroup.LayoutParams> Modifier.thenUnitLayoutAttribute(key: Any, reuseSupported: Boolean = true, applier: LP.(View) -> Unit): Modifier {
    return this.then(
        LayoutAttribute(key, object : LayoutAttributeApplier<LP, Unit, View> {
            override fun apply(target: LP, value: Unit, view: View) {
                applier(target, view)
            }
        }, Unit)
    )
}

interface ViewAwareModifier : Modifier.Element {
    fun onAttach(view: View)
}

class RefModifier(val block: (View) -> Unit) : ViewAwareModifier {
    override fun onAttach(view: View) {
        block(view)
    }
}

fun Modifier.ref(block: (View) -> Unit): Modifier = this.then(RefModifier(block))