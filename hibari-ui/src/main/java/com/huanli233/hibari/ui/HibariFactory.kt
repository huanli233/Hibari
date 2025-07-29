@file:Suppress("FunctionName")
@file:JvmName("HibariFactory")

package com.huanli233.hibari.ui

//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import com.huanli233.hibari.runtime.bypass.XmlBlockBypass
//
//internal const val ANDROID_WIDGET_CLASS_PREFIX = "android.widget."
//
///**
// * The Hibari factory type.
// *
// * - `parent` the parent view group.
// * - `base` the base view (from previous [HibariFactory] processed, if not will null).
// * - `context` the view context.
// * - `params` the parameters.
// */
//typealias HibariFactory = (parent: ViewGroup?, base: View?, context: Context, params: ViewCreatingParams) -> View?
//
///**
// * Create a Hibari factory.
// * @param createView the view creator.
// * @return [HibariFactory]
// */
//@JvmSynthetic
//fun HibariFactory(createView: HibariFactory) = createView
//
///**
// * Create a Hibari factory from [LayoutInflater].
// *
// * This will proxy the function of [LayoutInflater.Factory2] to [Hikage].
// * @param inflater the layout inflater.
// * @return [HibariFactory]
// */
//@JvmSynthetic
//fun HibariFactory(inflater: LayoutInflater) = HibariFactory { parent, base, context, params ->
//    base ?: inflater.factory2?.onCreateView(parent, params.viewClass.name.let {
//        if (it.startsWith(ANDROID_WIDGET_CLASS_PREFIX))
//            it.replace(ANDROID_WIDGET_CLASS_PREFIX, "")
//        else it
//    }, context, params.attrs ?: XmlBlockBypass.newAttrSet(context))
//}
//
///**
// * The [HibariFactory] builder.
// */
//class HibariFactoryBuilder() {
//
//    companion object {
//
//        /**
//         * Create a [HibariFactoryBuilder].
//         * @param builder the builder.
//         * @return [HibariFactoryBuilder]
//         */
//        inline fun create(builder: HibariFactoryBuilder.() -> Unit) = HibariFactoryBuilder().apply(builder)
//    }
//
//    /** Caches factories. */
//    private val factories = mutableListOf<HibariFactory>()
//
//    /**
//     * Add a factory.
//     * @param factory the factory.
//     */
//    fun add(factory: HibariFactory) {
//        factories.add(factory)
//    }
//
//    /**
//     * Add factories.
//     * @param factories the factories.
//     */
//    fun addAll(factories: List<HibariFactory>) {
//        this.factories.addAll(factories)
//    }
//
//    /**
//     * Build the factory.
//     * @return <[List]>[HibariFactory]
//     */
//    @PublishedApi
//    internal fun build() = factories.toList()
//}