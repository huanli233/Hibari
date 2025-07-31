package com.huanli233.hibari.runtime

interface TunationLocalsMap : MutableMap<TunationLocal<Any?>, ArrayDeque<ValueHolder<out Any?>>>, TunationLocalAccessorScope {

    @Suppress("UNCHECKED_CAST")
    override val <T> TunationLocal<T>.currentValue: T
        get() {
            val stack = this@TunationLocalsMap[this as TunationLocal<Any?>]

            val holder = stack?.firstOrNull() ?: this.defaultValueHolder

            return (holder as ValueHolder<T>).readValue(this@TunationLocalsMap as TunationLocalsHashMap)
        }
}

class TunationLocalsHashMap(initialCapacity: Int = 0, factor: Float = 0.75f) :
    HashMap<TunationLocal<Any?>, ArrayDeque<ValueHolder<out Any?>>>(initialCapacity, factor), TunationLocalsMap

fun tunationLocalHashMapOf() = TunationLocalsHashMap()

fun tunationLocalHashMapOf(
    vararg pairs: Pair<TunationLocal<Any?>, ArrayDeque<ValueHolder<out Any?>>>
): TunationLocalsHashMap = TunationLocalsHashMap(pairs.size).apply {
    putAll(pairs)
}

sealed class TunationLocal<T>(defaultFactory: () -> T) {
    internal open val defaultValueHolder: ValueHolder<T> = LazyValueHolder(defaultFactory)

    internal abstract fun updatedStateOf(
        value: ProvidedValue<T>,
        previous: ValueHolder<T>?
    ): ValueHolder<T>

    inline val current: T
        @Tunable get() = currentTuner.consume(this)
}

@Stable
abstract class ProvidableTunationLocal<T> internal constructor(defaultFactory: () -> T) :
    TunationLocal<T>(defaultFactory) {
    internal abstract fun defaultProvidedValue(value: T): ProvidedValue<T>

    infix fun provides(value: T) = defaultProvidedValue(value)

    infix fun providesDefault(value: T) = defaultProvidedValue(value).ifNotAlreadyProvided()

    infix fun providesComputed(compute: TunationLocalAccessorScope.() -> T) =
        ProvidedValue(
            tunationLocal = this,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = null,
            compute = compute,
            isDynamic = false
        )

    override fun updatedStateOf(
        value: ProvidedValue<T>,
        previous: ValueHolder<T>?
    ): ValueHolder<T> {
        return when (previous) {
            is DynamicValueHolder ->
                if (value.isDynamic) {
                    previous.state.value = value.effectiveValue
                    previous
                } else null
            is StaticValueHolder ->
                if (value.isStatic && value.effectiveValue == previous.value)
                    previous
                else null
            is ComputedValueHolder ->
                if (value.compute === previous.compute)
                    previous
                else null
            else -> null
        } ?: valueHolderOf(value)
    }

    private fun valueHolderOf(value: ProvidedValue<T>): ValueHolder<T> =
        when {
            value.isDynamic ->
                DynamicValueHolder(
                    value.state
                        ?: mutableStateOf(value.value, value.mutationPolicy
                            ?: structuralEqualityPolicy()
                        )
                )
            value.compute != null -> ComputedValueHolder(value.compute)
            value.state != null -> DynamicValueHolder(value.state)
            else -> StaticValueHolder(value.effectiveValue)
        }
}

internal class DynamicProvidableTunationLocal<T>(
    private val policy: SnapshotMutationPolicy<T>,
    defaultFactory: () -> T
) : ProvidableTunationLocal<T>(defaultFactory) {

    override fun defaultProvidedValue(value: T) =
        ProvidedValue(
            tunationLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = policy,
            state = null,
            compute = null,
            isDynamic = true
        )
}

internal class StaticProvidableTunationLocal<T>(defaultFactory: () -> T) :
    ProvidableTunationLocal<T>(defaultFactory) {

    override fun defaultProvidedValue(value: T) =
        ProvidedValue(
            tunationLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = false
        )
}

fun <T> tunationLocalOf(
    policy: SnapshotMutationPolicy<T> =
        structuralEqualityPolicy(),
    defaultFactory: () -> T
): ProvidableTunationLocal<T> = DynamicProvidableTunationLocal(policy, defaultFactory)

fun <T> staticTunationLocalOf(defaultFactory: () -> T): ProvidableTunationLocal<T> =
    StaticProvidableTunationLocal(defaultFactory)

fun <T> compositionLocalWithComputedDefaultOf(
    defaultComputation: TunationLocalAccessorScope.() -> T
): ProvidableTunationLocal<T> =
    ComputedProvidableTunationLocal(defaultComputation)

internal class ComputedProvidableTunationLocal<T>(
    defaultComputation: TunationLocalAccessorScope.() -> T
) : ProvidableTunationLocal<T>({ hibariRuntimeError("Unexpected call to default provider") }) {
    override val defaultValueHolder = ComputedValueHolder(defaultComputation)

    override fun defaultProvidedValue(value: T): ProvidedValue<T> =
        ProvidedValue(
            tunationLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = true
        )
}

interface TunationLocalAccessorScope {
    val <T> TunationLocal<T>.currentValue: T
}