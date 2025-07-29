package com.huanli233.hibari.runtime

import com.huanli233.hibari.ui.node.Node
import kotlinx.coroutines.sync.Mutex

fun hibariRuntimeError(message: String): Nothing = error(message)

class ProvidedValue<T> internal constructor(
    /**
     * The composition local that is provided by this value. This is the left-hand side of the
     * [ProvidableTunationLocal.provides] infix operator.
     */
    val tunationLocal: TunationLocal<T>,
    value: T?,
    private val explicitNull: Boolean,
    internal val mutationPolicy: SnapshotMutationPolicy<T>?,
    internal val state: MutableState<T>?,
    internal val compute: (TunationLocalAccessorScope.() -> T)?,
    internal val isDynamic: Boolean
) {
    private val providedValue: T? = value

    @Suppress("UNCHECKED_CAST")
    val value: T get() = providedValue as T

    @get:JvmName("getCanOverride")
    var canOverride: Boolean = true
        private set
    @Suppress("UNCHECKED_CAST")
    internal val effectiveValue: T
        get() = when {
            explicitNull -> null as T
            state != null -> state.value
            providedValue != null -> providedValue
            else -> hibariRuntimeError("Unexpected form of a provided value")
        }
    internal val isStatic get() = (explicitNull || value != null) && !isDynamic

    internal fun ifNotAlreadyProvided() = this.also { canOverride = false }
}

open class Tuner {

    val mutex = Mutex()
    val walker = Walker()
    val memory = mutableMapOf<String, Any?>()
    internal val localValueStacks = tunationLocalHashMapOf()

    fun startGroup(key: Int) {
        walker.start(key)
    }

    fun endGroup(key: Int) {
        walker.end()
    }

    fun rememberedValue(): Any? {
        return memory[walker.path()]
    }

    fun updateRememberedValue(value: Any?) {
        memory[walker.path()] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun startProviders(values: Array<out ProvidedValue<*>>) {
        values.forEach { providedValue ->
            val local = providedValue.tunationLocal as ProvidableTunationLocal<Any?>
            val stack = localValueStacks.getOrPut(local) { ArrayDeque() }
            val previousHolder = stack.firstOrNull()

            val newHolder = local.updatedStateOf(
                providedValue as ProvidedValue<Any?>,
                previousHolder as ValueHolder<Any?>
            )
            stack.addFirst(newHolder)
        }
    }

    fun endProviders(values: Array<out ProvidedValue<*>>) {
        values.forEach { providedValue ->
            val stack = localValueStacks[providedValue.tunationLocal as TunationLocal<Any?>]
            stack?.removeFirstOrNull()
        }
    }

    fun <T> consume(tunationLocal: TunationLocal<T>): T {
        return with(localValueStacks) { tunationLocal.currentValue }
    }

    fun runTunable(content: @Tunable () -> Unit) {
        content()
    }

    fun emitNode(node: Node) {

    }
}