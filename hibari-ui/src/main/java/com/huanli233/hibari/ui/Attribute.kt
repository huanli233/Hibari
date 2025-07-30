package com.huanli233.hibari.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.XmlRes

data class AttributeKey<T>(val name: String)

interface AttributeApplier<in V, in T> {
    fun apply(target: V, value: T)
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
    val applier: AttributeApplier<V, T>,
    override val value: T,
    override val reuseSupported: Boolean = true
) : Attribute<T> {
    override val key: Any get() = applier

    override fun applyTo(view: View) {
        @Suppress("UNCHECKED_CAST")
        applier.apply(view as V, value)
    }
}

data class LayoutAttribute<LP : ViewGroup.LayoutParams, T>(
    val applier: AttributeApplier<LP, T>,
    override val value: T,
    override val reuseSupported: Boolean = true
) : Attribute<T> {
    override val key: Any get() = applier

    override fun applyTo(view: View) {
        view.layoutParams?.let {
            @Suppress("UNCHECKED_CAST")
            applier.apply(it as LP, value)
        }
    }
}

object TextViewTextApplier : AttributeApplier<TextView, CharSequence> {
    override fun apply(target: TextView, value: CharSequence) {
        target.text = value
    }
}

fun Modifier.text(text: CharSequence): Modifier {
    return this.then(ViewAttribute(TextViewTextApplier, text))
}

data class ViewClassAttribute(val viewClass: Class<out View>) : Modifier.Element {
    val key: Any = ViewClassAttribute::class
}

fun Modifier.viewClass(clazz: Class<out View>): Modifier {
    return this.then(ViewClassAttribute(clazz))
}

data class AttrsAttribute(val attrs: Int) : Modifier.Element {
    val key: Any = AttrsAttribute::class
}

fun Modifier.attrs(@XmlRes attrs: Int): Modifier {
    return this.then(AttrsAttribute(attrs))
}