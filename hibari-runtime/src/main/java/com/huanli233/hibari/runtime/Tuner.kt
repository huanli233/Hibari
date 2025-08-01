package com.huanli233.hibari.runtime

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.huanli233.hibari.runtime.snapshots.Snapshot
import com.huanli233.hibari.ui.HibariFactory
import com.huanli233.hibari.ui.node.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.RuntimeException

fun hibariRuntimeError(message: String, cause: Throwable? = null): Nothing = throw HibariRuntimeError(message, cause)

class HibariRuntimeError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

val hibariViewId = ViewCompat.generateViewId()
val subcomposeLayoutId = ViewCompat.generateViewId()
@SuppressLint("PrivateApi")
fun invokeSetKeyedTag(view: View, key: Int, tag: Any?) {
    try {
        val viewClass = View::class.java
        val method = if (Build.VERSION.SDK_INT >= 28) {
            HiddenApiBypass.getDeclaredMethod(viewClass, "setKeyedTag", Int::class.java, Any::class.java)
        } else {
            viewClass.getDeclaredMethod("setKeyedTag", Int::class.java, Any::class.java)
        }
        method.isAccessible = true
        method.invoke(view, key, tag)
    } catch (e: Exception) {
        println("Error invoking setKeyedTag: ${e.message}")
        e.printStackTrace()
    }
}

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

open class Tuner(
    val tunation: Tunation,
    val onReadState: (Any) -> Unit = {
        Log.d("Tuner", "onReadState: $it")
        SnapshotManager.recordRead(tunation, it)
    }
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val nodeStack = ArrayDeque<MutableList<Node>>()
    val rootNodes: List<Node>
        get() = nodeStack.firstOrNull() ?: emptyList()

    val walker = Walker()
    var memory = mutableMapOf<String, Any?>()
    internal val localValueStacks = tunationLocalHashMapOf()

    fun dispose() {
        memory.values.forEach { value ->
            val rememberedValue = (value as? Pair<*, *>)?.second
            (rememberedValue as? RememberObserver)?.onForgotten()
        }
        memory.clear()
        coroutineScope.cancel()
    }

    fun startGroup(key: Int) {
        walker.start(key)
    }

    fun endGroup(key: Int) {
        walker.end()
    }

    fun startComposition() {
        nodeStack.addFirst(mutableListOf())
    }

    fun endComposition(): List<Node> {
        if (nodeStack.size != 1) {
            hibariRuntimeError("Composition stack imbalance. Mismatched start/end calls.")
        }
        return nodeStack.removeFirst()
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
                previousHolder as? ValueHolder<Any?>
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

    fun runTunable(tunation: Tunation) {
        walker.clear()
        SnapshotManager.clearDependencies(tunation)
        Snapshot.observe(
            readObserver = {
                onReadState(it)
            }
        ) {
            runTunable(tunation.content)
        }
    }

    @Tunable
    fun emitNode(node: Node, content: @Tunable () -> Unit = {}): Node {
        node.key = walker.path()
        nodeStack.addFirst(mutableListOf())
        runTunable(content)
        val children = nodeStack.removeFirst()
        node.children = children
        nodeStack.firstOrNull()?.add(node) ?: hibariRuntimeError("Cannot emit node outside of a composition.")
        return node
    }
}