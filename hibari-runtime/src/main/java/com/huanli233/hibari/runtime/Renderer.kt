package com.huanli233.hibari.runtime

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import android.view.View
import android.view.ViewGroup
import androidx.annotation.XmlRes
import com.highcapable.yukireflection.factory.constructor
import com.highcapable.yukireflection.factory.notExtends
import com.highcapable.yukireflection.type.android.AttributeSetClass
import com.highcapable.yukireflection.type.android.ContextClass
import com.huanli233.hibari.runtime.bypass.XmlBlockBypass
import com.huanli233.hibari.ui.Attribute
import com.huanli233.hibari.ui.AttributeHandlerRegistry
import com.huanli233.hibari.ui.AttributeKeys
import com.huanli233.hibari.ui.HibariFactory
import com.huanli233.hibari.ui.ViewAttribute
import com.huanli233.hibari.ui.ViewClassAttribute
import com.huanli233.hibari.ui.ViewCreatingParams
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.viewClass
import org.xmlpull.v1.XmlPullParser
import java.lang.reflect.Constructor
import java.util.concurrent.atomic.AtomicInteger

class Renderer(
    val factories: List<HibariFactory>,
    val parent: ViewGroup
) {
    companion object {
        private val viewConstructors = mutableMapOf<String, ViewConstructor>()
        internal var attrSets = mutableMapOf<Int, AttributeSet>()
        private val viewAtomicId = AtomicInteger(0x7F00000)
    }

    fun render(node: Node): View {
        val modifierAttrs = node.modifier.flattenToList().associateBy { it.key }

        Log.d("huanli233", modifierAttrs.keys.joinToString())

        val viewClass = (modifierAttrs[AttributeKeys.viewClass] as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val id = (modifierAttrs[AttributeKeys.id] as? ViewAttribute<*>)?.value as? String
        val attrXml = ((modifierAttrs[AttributeKeys.attributeSet] as? ViewAttribute<*>)?.value as? Int) ?: -1

        val attrs = createAttributeSet(parent.context, attrXml)
        val view = createViewFromFactory(viewClass, id, parent.context, attrs) ?: getViewConstructor(viewClass, attrXml != -1)?.build(parent.context, attrs)
        view?.let { applyAttributes(it, node.modifier.flattenToList()) }
        return view ?: hibariRuntimeError("The view class ${viewClass.name} must have a constructor with two parameters of type Context and AttributeSet to apply attributes.")
    }

    fun applyAttributes(view: View, attrs: List<Attribute>) {
        attrs.forEach {
            AttributeHandlerRegistry.find(view::class.java, it.key)?.let { handler ->
                when (it) {
                    is ViewAttribute<*> -> {
                        handler.apply(view, it.value as Any)
                    }
                }
            }
        }
    }

    private fun <V : View> getViewConstructor(
        viewClass: Class<V>,
        hasAttr: Boolean = false
    ): ViewConstructor? {
        val cacheKey = "${viewClass.name}-${hasAttr}"
        return viewConstructors[cacheKey] ?: run {
            var parameterCount = 0
            val twoParams = viewClass.constructor {
                param(ContextClass, AttributeSetClass)
            }.ignored().give()
            val onceParam = viewClass.constructor {
                param(ContextClass)
            }.ignored().give()
            val constructor = if (hasAttr) {
                twoParams?.apply { parameterCount = 2 } ?: hibariRuntimeError("The view class ${viewClass.name} must have a constructor with two parameters of type Context and AttributeSet to apply attributes.")
            } else {
                onceParam?.apply { parameterCount = 1 }
                    ?: twoParams?.apply { parameterCount = 2 }
            }
            val viewConstructor = constructor?.let { ViewConstructor(it, parameterCount) }
            if (viewConstructor != null) viewConstructors[viewClass.name] = viewConstructor
            viewConstructor
        }
    }

    private fun <V : View> createViewFromFactory(viewClass: Class<V>, id: String?, context: Context, attrs: AttributeSet): V? {
        var processed: V? = null
        factories.forEach { factory ->
            val params = ViewCreatingParams(viewClass as Class<*>, attrs)
            val view = factory(parent, processed, context, params)
            if (view != null && view.javaClass notExtends viewClass) hibariRuntimeError(
                "HikageFactory cannot cast the created view type \"${view.javaClass}\" to \"${viewClass.name}\", " +
                        "please confirm that the view type you created is correct."
            )
            @Suppress("UNCHECKED_CAST")
            if (view != null) processed = view as? V?
        }; return processed
    }

    internal fun createAttributeSet(context: Context, @XmlRes attrXml: Int): AttributeSet =
        attrSets.getOrPut(attrXml) {
            if (attrXml == -1) {
                XmlBlockBypass.newAttrSet(context)
            } else {
                runCatching {
                    val parser = context.resources.getXml(attrXml)
                    var type = parser.eventType
                    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
                        type = parser.next()
                    }
                    if (type != XmlPullParser.START_TAG) {
                        hibariRuntimeError("No start tag found for XML resource $attrXml")
                    }
                    Xml.asAttributeSet(parser)
                }.getOrElse { hibariRuntimeError("Failed to create attribute set", it) }
            }
        }

    private inner class ViewConstructor(
        private val instance: Constructor<*>,
        private val parameterCount: Int
    ) {

        /**
         * Build the view.
         * @param context the context.
         * @param attrs the attribute set.
         * @return [V] or null.
         */
        fun <V : View> build(
            context: Context,
            attrs: AttributeSet
        ) = when (parameterCount) {
            2 -> instance.newInstance(context, attrs)
            1 -> instance.newInstance(context)
            else -> null
        } as? V?
    }

}