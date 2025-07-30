package com.huanli233.hibari.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

data class AttributeKey<T>(val name: String)

interface AttributeHandler<in V : View, in T> {
    fun apply(view: V, value: T)
}

object AttributeHandlerRegistry {

    private val handlers = mutableMapOf<Class<out View>, MutableMap<AttributeKey<*>, AttributeHandler<*, *>>>()

    init {
        register<TextView, String>(TextViewAttributeKeys.text) { v, t ->
            v.text = t
        }
    }

    fun <V : View, T> register(
        viewClass: Class<V>,
        attributeName: AttributeKey<*>,
        handler: AttributeHandler<V, T>
    ) {
        handlers.getOrPut(viewClass) { mutableMapOf() }[attributeName] = handler
    }

    inline fun <reified V : View, T> register(
        attributeName: AttributeKey<*>,
        crossinline block: (view: V, value: T) -> Unit
    ) {
        register(V::class.java, attributeName, object : AttributeHandler<V, T> {
            override fun apply(view: V, value: T) {
                block(view, value)
            }
        })
    }

    fun find(viewClass: Class<out View>, attributeName: AttributeKey<*>): AttributeHandler<View, Any>? {
        var currentClass: Class<*>? = viewClass
        while (currentClass != null && View::class.java.isAssignableFrom(currentClass)) {
            handlers[currentClass]?.get(attributeName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it as AttributeHandler<View, Any>
            }
            currentClass = currentClass.superclass
        }
        return null
    }
}

interface LayoutAttributeHandler<in V : ViewGroup.LayoutParams, in T> {
    fun apply(view: V, value: T)
}

object LayoutAttributeHandlerRegistry {
    private val handlers = mutableMapOf<Class<out ViewGroup.LayoutParams>, MutableMap<AttributeKey<*>, LayoutAttributeHandler<*, *>>>()

    fun <V : ViewGroup.LayoutParams, T> register(
        viewClass: Class<V>,
        attributeName: AttributeKey<*>,
        handler: LayoutAttributeHandler<V, T>
    ) {
        handlers.getOrPut(viewClass) { mutableMapOf() }[attributeName] = handler
    }

    inline fun <reified V : ViewGroup.LayoutParams, T> register(
        attributeName: AttributeKey<*>,
        crossinline block: (view: V, value: T) -> Unit
    ) {
        register(V::class.java, attributeName, object : LayoutAttributeHandler<V, T> {
            override fun apply(view: V, value: T) {
                block(view, value)
            }
        })
    }

    fun find(lparamsClass: Class<out ViewGroup.LayoutParams>, attributeName: AttributeKey<*>): AttributeHandler<View, Any>? {
        var currentClass: Class<*>? = lparamsClass
        while (currentClass != null && View::class.java.isAssignableFrom(currentClass)) {
            handlers[currentClass]?.get(attributeName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it as AttributeHandler<View, Any>
            }
            currentClass = currentClass.superclass
        }
        return null
    }
}

/**
 * An atomic configuration unit for a LayoutNode, which will be translated
 * into a View property or a specific setup action by the Renderer.
 */
interface Attribute : Modifier.Element {
    /**
     * A unique key for this attribute type. Used during diffing to identify
     * if an attribute of the same type already exists.
     */
    val key: AttributeKey<*>

    /**
     * If true, the Renderer can update the View with the new attribute value
     * without recreating the View. If false, a change in this attribute
     * might force a View recreation depending on the Renderer's logic.
     */
    val reuseSupported: Boolean
}

data class ViewClassAttribute(val viewClass: Class<out View>) : Attribute {
    override val key get() = AttributeKeys.viewClass
    override val reuseSupported: Boolean get() = false
}

data class ViewAttribute<T>(
    override val key: AttributeKey<T>,
    val value: T?,
    override val reuseSupported: Boolean = true
) : Attribute

fun <T> Modifier.attribute(key: AttributeKey<T>, value: T?, reuseSupported: Boolean = true): Modifier {
    return this.then(ViewAttribute(key, value, reuseSupported))
}

fun Modifier.viewClass(clazz: Class<out View>): Modifier {
    return this.then(ViewClassAttribute(clazz))
}