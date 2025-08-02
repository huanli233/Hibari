package com.huanli233.hibari.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Xml
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.getChildMeasureSpec
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
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.RefModifier
import com.huanli233.hibari.ui.ViewClassAttribute
import com.huanli233.hibari.ui.ViewCreatingParams
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.layout.ParentDataModifier
import com.huanli233.hibari.ui.node.Measurable
import com.huanli233.hibari.ui.node.MeasurePolicy
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.node.Placeable
import com.huanli233.hibari.ui.unit.Constraints
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
        fun generateViewId(id: String?): Pair<String, Int> {
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

        fun generateRandomViewId() = "anonymous@${UUID.randomUUID()}"
    }

    fun render(node: Node, parent: ViewGroup): View {
        if (node.measurePolicy != null) {
            return LayoutNodeHost(parent.context).apply {
                this.node = node
                invokeSetKeyedTag(this, hibariNodeKey, node.key)
            }
        }

        val modifierAttrs = node.modifier.flattenToList()

        val viewClass = (modifierAttrs.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val attrXml = (modifierAttrs.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1
        val id = (modifierAttrs.firstOrNull { it is IdAttribute } as? IdAttribute)?.id
        val intId = (modifierAttrs.firstOrNull { it is IntIdAttribute } as? IntIdAttribute)?.id

        val (_, viewId) = generateViewId(id)

        val attrs = createAttributeSet(parent.context, attrXml)
        val view = createViewFromFactory(viewClass, parent.context, attrs) ?: getViewConstructor(viewClass, attrXml != -1)?.build(parent.context, attrs)

        if (intId != null) {
            view?.id = intId
        } else {
            view?.id = viewId
        }
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

const val LayoutParamsMatchParent = ViewGroup.LayoutParams.MATCH_PARENT

const val LayoutParamsWrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

class LayoutParams private constructor(
    private val lpClass: Class<ViewGroup.LayoutParams>,
    private val parent: ViewGroup?
) {

    private class BodyBuilder(
        val width: Int,
        val height: Int,
        val matchParent: Boolean,
        val widthMatchParent: Boolean,
        val heightMatchParent: Boolean,
    )

    private class WrapperBuilder(
        val delegate: LayoutParams?,
        val lparams: ViewGroup.LayoutParams?
    )

    private var bodyBuilder: BodyBuilder? = null

    private var wrapperBuilder: WrapperBuilder? = null

    @PublishedApi
    internal companion object {

        @Suppress("UNCHECKED_CAST")
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

        @Suppress("UNCHECKED_CAST")
        fun <LP : ViewGroup.LayoutParams> from(
            lpClass: Class<LP>,
            parent: ViewGroup?,
            delegate: LayoutParams?,
            lparams: ViewGroup.LayoutParams? = null
        ) = LayoutParams(lpClass as Class<ViewGroup.LayoutParams>, parent).apply {
            wrapperBuilder = WrapperBuilder(delegate, lparams)
        }
    }

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
            ?: lpClass.buildOf<ViewGroup.LayoutParams>(LayoutParamsWrapContent, LayoutParamsWrapContent) {
                param(IntType, IntType)
            } ?: hibariRuntimeError("Create default layout params failed.")
    }

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

fun Constraints.Companion.fromMeasureSpec(widthMeasureSpec: Int, heightMeasureSpec: Int): Constraints {
    val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
    val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
    val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
    val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

    val minWidth = if (widthMode == View.MeasureSpec.EXACTLY) widthSize else 0
    val maxWidth = if (widthMode == View.MeasureSpec.UNSPECIFIED) Constraints.Infinity else widthSize
    val minHeight = if (heightMode == View.MeasureSpec.EXACTLY) heightSize else 0
    val maxHeight = if (heightMode == View.MeasureSpec.UNSPECIFIED) Constraints.Infinity else heightSize

    return Constraints(minWidth, maxWidth, minHeight, maxHeight)
}

interface LayoutModifierNode : Modifier.Element {
    fun measure(measurable: Measurable, constraints: Constraints): Placeable
}

internal class LayoutNodeHost(context: Context) : ViewGroup(context) {
    var node: Node? = null
        set(value) {
            field = value
            if (value != null) {
                measurePolicy = value.measurePolicy
            }
            requestLayout()
        }
    private var measurePolicy: MeasurePolicy? = null
    private var rootPlaceable: Placeable? = null
    private val childMeasurables = mutableListOf<ViewMeasurable>()

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val policy = measurePolicy
        if (policy == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        if (childMeasurables.size != childCount) {
            childMeasurables.clear()
            (0 until childCount).forEach { i ->
                val childView = getChildAt(i)
                val childNode = node?.children?.getOrNull(i)
                childMeasurables.add(ViewMeasurable(childView, childNode))
            }
        }

        childMeasurables.forEach { it.invalidateMeasureCache() }

        val constraints = Constraints.fromMeasureSpec(widthMeasureSpec, heightMeasureSpec)
        rootPlaceable = policy.measure(childMeasurables, constraints)
        setMeasuredDimension(rootPlaceable!!.width, rootPlaceable!!.height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        rootPlaceable?.placeAt(0, 0)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
}

open class ViewMeasurable(
    val view: View,
    private val node: Node?
) : Measurable {

    private val measurementCache = mutableMapOf<Constraints, Placeable>()
    private val chainedMeasure: (Constraints) -> Placeable

    override val context: Context
        get() = view.context

    override val parentData: Any? by lazy {
        node?.modifier
            ?.flattenToList()
            ?.filterIsInstance<ParentDataModifier>()
            ?.fold(null as Any?) { currentData, modifier ->
                modifier.modifyParentData(currentData)
            }
    }

    init {
        val layoutModifiers = node?.modifier?.flattenToList()?.filterIsInstance<LayoutModifierNode>() ?: emptyList()

        val baseMeasure: (Constraints) -> Placeable = { c ->
            BasePlaceable(view, c)
        }
        chainedMeasure = layoutModifiers.foldRight(baseMeasure) { modifier, next ->
            { c ->
                val measurableProxy = object : Measurable {
                    override val parentData: Any? = this@ViewMeasurable.parentData
                    override val context: Context = this@ViewMeasurable.context
                    override fun measure(constraints: Constraints): Placeable {
                        return next(constraints)
                    }
                }
                modifier.measure(measurableProxy, c)
            }
        }
    }

    override fun measure(constraints: Constraints): Placeable {
        return measurementCache.getOrPut(constraints) {
            chainedMeasure(constraints)
        }
    }

    fun invalidateMeasureCache() {
        measurementCache.clear()
    }
}

private class BasePlaceable(
    private val view: View,
    constraints: Constraints
) : Placeable() {
    init {
        if (view is LayoutNodeHost) {
            view.measure(
                constraints.toWidthMeasureSpec(),
                constraints.toHeightMeasureSpec()
            )
        } else {
            val lp = view.layoutParams
            val childWidthMeasureSpec = getChildMeasureSpec(
                constraints.toWidthMeasureSpec(),
                view.paddingLeft + view.paddingRight,
                lp.width
            )
            val childHeightMeasureSpec = getChildMeasureSpec(
                constraints.toHeightMeasureSpec(),
                view.paddingTop + view.paddingBottom,
                lp.height
            )
            view.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
        this.width = view.measuredWidth
        this.height = view.measuredHeight
    }

    override fun placeAt(x: Int, y: Int) {
        view.layout(x, y, x + width, y + height)
    }
}