package com.huanli233.hibari.runtime

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.annotation.XmlRes
import androidx.core.view.ViewCompat
import com.highcapable.yukireflection.factory.buildOf
import com.highcapable.yukireflection.factory.constructor
import com.highcapable.yukireflection.factory.current
import com.highcapable.yukireflection.factory.notExtends
import com.highcapable.yukireflection.type.android.AttributeSetClass
import com.highcapable.yukireflection.type.android.ContextClass
import com.highcapable.yukireflection.type.android.ViewGroup_LayoutParamsClass
import com.highcapable.yukireflection.type.java.IntType
import com.huanli233.hibari.runtime.bypass.XmlBlockBypass
import com.huanli233.hibari.ui.Attribute
import com.huanli233.hibari.ui.AttrsAttribute
import com.huanli233.hibari.ui.HibariFactory
import com.huanli233.hibari.ui.RefModifier
import com.huanli233.hibari.ui.ViewClassAttribute
import com.huanli233.hibari.ui.ViewCreatingParams
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.node.Node
import org.xmlpull.v1.XmlPullParser
import java.lang.reflect.Constructor
import java.util.UUID

class Renderer(
    val factories: List<HibariFactory>,
    val parent: ViewGroup
) {
    companion object {
        private val viewConstructors = mutableMapOf<String, ViewConstructor>()
        internal var attrSets = mutableMapOf<Int, AttributeSet>()

        val hibariNodeKey = ViewCompat.generateViewId()

        val viewIds = mutableMapOf<String, Int>()
        /**
         * Generate view id from [id].
         * @param id the view id.
         * @return [Pair]<[String], [Int]>
         */
        fun generateViewId(id: String?): Pair<String, Int> {
            /**
             * Generate a new view id.
             * @param id the view id.
             * @return [Int]
             */
            fun doGenerate(id: String): Int {
                val generateId = ViewCompat.generateViewId()
                return viewIds.getOrPut(id) {
                    generateId
                }
            }
            synchronized(this) {
                val requireId = id ?: generateRandomViewId()
                val viewId = doGenerate(requireId)
                return requireId to viewId
            }
        }

        internal fun generateRandomViewId() = "anonymous@${UUID.randomUUID()}"
    }

    fun render(node: Node, parent: ViewGroup): View {
        val modifierAttrs = node.modifier.flattenToList()

        val viewClass = (modifierAttrs.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val attrXml = (modifierAttrs.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1
        val id = (modifierAttrs.firstOrNull { it is IdAttribute } as? IdAttribute)?.id

        val (_, viewId) = generateViewId(id)

        val attrs = createAttributeSet(parent.context, attrXml)
        val view = createViewFromFactory(viewClass, parent.context, attrs) ?: getViewConstructor(viewClass, attrXml != -1)?.build(parent.context, attrs)

        view?.id = viewId
        view?.let { view ->
            invokeSetKeyedTag(view, hibariViewId, id)

            val lpClass = try {
                Class.forName("${parent.javaClass.name}\$LayoutParams")
            } catch (_: ClassNotFoundException) {
                null
            }
            view.layoutParams = lpClass?.let {
                it.constructor { param(IntType, IntType) }.ignored().give()
                    ?.newInstance(
                        LayoutParamsWrapContent,
                        LayoutParamsWrapContent
                    ) as ViewGroup.LayoutParams
            } ?: parent.current(ignored = true).method {
                name = "generateDefaultLayoutParams"
                emptyParam()
                superClass()
            }.invoke() ?: ViewGroup.LayoutParams(
                LayoutParamsWrapContent,
                LayoutParamsWrapContent
            )

            invokeSetKeyedTag(view, hibariNodeKey, node.key)

            applyAttributes(view, modifierAttrs.mapNotNull { it as? Attribute<*> })
            modifierAttrs.filter { it is RefModifier }.map { (it as? RefModifier)?.block }.forEach {
                it?.invoke(view)
            }
        }

        return view ?: hibariRuntimeError("The view class ${viewClass.name} must have a constructor with two parameters of type Context and AttributeSet to apply attributes.")
    }

    fun applyAttributes(view: View, attrs: List<Attribute<*>>) {
        attrs.forEach {
            it.applyTo(view)
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

    private fun <V : View> createViewFromFactory(viewClass: Class<V>, context: Context, attrs: AttributeSet): V? {
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
        @Suppress("UNCHECKED_CAST")
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

/** Reference to [ViewGroup.LayoutParams.MATCH_PARENT]. */
const val LayoutParamsMatchParent = ViewGroup.LayoutParams.MATCH_PARENT

/** Reference to [ViewGroup.LayoutParams.WRAP_CONTENT]. */
const val LayoutParamsWrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

/**
 * The [Hikage] layout params.
 * @see ViewLayoutParams
 * @param current the current [Hikage].
 * @param lpClass the layout params type.
 * @param parent the parent view group.
 */
class LayoutParams private constructor(
    private val lpClass: Class<ViewGroup.LayoutParams>,
    private val parent: ViewGroup?
) {

    /**
     * Builder params of body.
     */
    private class BodyBuilder(
        val width: Int,
        val height: Int,
        val matchParent: Boolean,
        val widthMatchParent: Boolean,
        val heightMatchParent: Boolean,
    )

    /**
     * Builder params of wrapper.
     */
    private class WrapperBuilder(
        val delegate: LayoutParams?,
        val lparams: ViewGroup.LayoutParams?
    )

    /** The layout params body. */
    private var bodyBuilder: BodyBuilder? = null

    /** The layout params wrapper. */
    private var wrapperBuilder: WrapperBuilder? = null

    @PublishedApi
    internal companion object {

        /**
         * Create a new [LayoutParams]
         * @see ViewLayoutParams
         * @param current the current [Hikage].
         * @param parent the parent view group.
         * @return [LayoutParams]
         */
        fun <LP : ViewGroup.LayoutParams> from(
            lpClass: Class<LP>,
            parent: ViewGroup?,
            width: Int,
            height: Int,
            matchParent: Boolean,
            widthMatchParent: Boolean,
            heightMatchParent: Boolean,
        ) = LayoutParams(lpClass as Class<ViewGroup.LayoutParams>, parent).apply {
            bodyBuilder = BodyBuilder(
                width, height, matchParent, widthMatchParent, heightMatchParent
            )
        }

        /**
         * Create a new [LayoutParams].
         * @param current the current [Hikage].
         * @param parent the parent view group.
         * @param delegate the delegate.
         * @param lparams the another layout params.
         * @return [LayoutParams]
         */
        fun <LP : ViewGroup.LayoutParams> from(
            lpClass: Class<LP>,
            parent: ViewGroup?,
            delegate: LayoutParams?,
            lparams: ViewGroup.LayoutParams? = null
        ) = LayoutParams(lpClass as Class<ViewGroup.LayoutParams>, parent).apply {
            wrapperBuilder = WrapperBuilder(delegate, lparams)
        }
    }

    /**
     * Create a default layout params.
     * @return [ViewGroup.LayoutParams]
     */
    private fun createDefaultLayoutParams(lparams: ViewGroup.LayoutParams? = null): ViewGroup.LayoutParams {
        if (lparams != null && lpClass.isInstance(lparams)) return lparams
        val wrapped = lparams?.let {
            parent?.current(ignored = true)?.method {
                name = "generateLayoutParams"
                param(ViewGroup_LayoutParamsClass)
                superClass()
            }?.invoke<ViewGroup.LayoutParams?>(it)
        }
        return wrapped
        // Build a default.
            ?: lpClass.buildOf<ViewGroup.LayoutParams>(LayoutParamsWrapContent, LayoutParamsWrapContent) {
                param(IntType, IntType)
            } ?: hibariRuntimeError("Create default layout params failed.")
    }

    /**
     * Create the layout params.
     * @return [ViewGroup.LayoutParams]
     */
    fun create(): ViewGroup.LayoutParams {
        if (bodyBuilder == null && wrapperBuilder == null) hibariRuntimeError("No layout params builder found.")
        return bodyBuilder?.let {
            val lparams = ViewLayoutParams(lpClass, it.width, it.height, it.matchParent, it.widthMatchParent, it.heightMatchParent)
            lparams
        } ?: wrapperBuilder?.let {
            val lparams = it.delegate?.create() ?: it.lparams
            createDefaultLayoutParams(lparams)
        } ?: hibariRuntimeError("Internal error of build layout params.")
    }
}

private const val LayoutParamsUnspecified = LayoutParamsWrapContent - 1

@JvmOverloads
fun <VGLP : ViewGroup.LayoutParams> ViewLayoutParams(
    lpClass: Class<VGLP>,
    @Px width: Int = LayoutParamsUnspecified,
    @Px height: Int = LayoutParamsUnspecified,
    matchParent: Boolean = false,
    widthMatchParent: Boolean = false,
    heightMatchParent: Boolean = false
): VGLP {
    val absWidth = when {
        width != LayoutParamsUnspecified -> width
        matchParent || widthMatchParent -> LayoutParamsMatchParent
        else -> LayoutParamsWrapContent
    }
    val absHeight = when {
        height != LayoutParamsUnspecified -> height
        matchParent || heightMatchParent -> LayoutParamsMatchParent
        else -> LayoutParamsWrapContent
    }
    return lpClass.buildOf<VGLP>(absWidth, absHeight) {
        param(IntType, IntType)
    } ?: error(
        "Create ViewGroup.LayoutParams failed. " +
                "Could not found the default constructor LayoutParams(width, height) in $lpClass."
    )
}