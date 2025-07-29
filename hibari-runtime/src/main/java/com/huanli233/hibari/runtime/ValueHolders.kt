package com.huanli233.hibari.runtime

interface ValueHolder<T> {
    fun readValue(map: TunationLocalsHashMap): T
    fun toProvided(local: TunationLocal<T>): ProvidedValue<T>
}

data class StaticValueHolder<T>(val value: T) : ValueHolder<T> {
    override fun readValue(map: TunationLocalsHashMap): T = value
    override fun toProvided(local: TunationLocal<T>): ProvidedValue<T> =
        ProvidedValue(
            tunationLocal = local,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = false
        )
}

class LazyValueHolder<T>(valueProducer: () -> T) : ValueHolder<T> {
    private val current by lazy(valueProducer)

    override fun readValue(map: TunationLocalsHashMap): T = current
    override fun toProvided(local: TunationLocal<T>): ProvidedValue<T> =
        hibariRuntimeError("Cannot produce a provider from a lazy value holder")
}

data class ComputedValueHolder<T>(
    val compute: TunationLocalAccessorScope.() -> T
) : ValueHolder<T> {
    override fun readValue(map: TunationLocalsHashMap): T =
        map.compute()
    override fun toProvided(local: TunationLocal<T>): ProvidedValue<T> =
        ProvidedValue(
            tunationLocal = local,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = null,
            compute = compute,
            isDynamic = false
        )
}

data class DynamicValueHolder<T>(val state: MutableState<T>) : ValueHolder<T> {
    override fun readValue(map: TunationLocalsHashMap): T = state.value
    override fun toProvided(local: TunationLocal<T>): ProvidedValue<T> =
        ProvidedValue(
            tunationLocal = local,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = state,
            compute = null,
            isDynamic = true
        )
}
