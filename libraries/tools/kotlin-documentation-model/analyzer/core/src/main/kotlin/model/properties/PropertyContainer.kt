package org.jetbrains.dokka.model.properties

data class PropertyContainer<C : Any> internal constructor(
    @PublishedApi internal val map: Map<ExtraProperty.Key<C, *>, ExtraProperty<C>>
) {
    operator fun <D : C> plus(prop: ExtraProperty<D>): PropertyContainer<D> =
        PropertyContainer(map + (prop.key to prop))

    // TODO: Add logic for caching calculated properties
    inline operator fun <reified T : Any> get(key: ExtraProperty.Key<C, T>): T? = when (val prop = map[key]) {
        is T? -> prop
        else -> throw ClassCastException("Property for $key stored under not matching key type.")
    }

    inline fun <reified T : Any> allOfType(): List<T> = map.values.filterIsInstance<T>()
    fun <D : C> addAll(extras: Collection<ExtraProperty<D>>): PropertyContainer<D> =
        PropertyContainer(map + extras.map { p -> p.key to p })

    companion object {
        fun <T : Any> empty(): PropertyContainer<T> = PropertyContainer(emptyMap())
        fun <T : Any> withAll(vararg extras: ExtraProperty<T>) = empty<T>().addAll(extras.toList())
        fun <T : Any> withAll(extras: Collection<ExtraProperty<T>>) = empty<T>().addAll(extras)
    }
}

operator fun <D: Any> PropertyContainer<D>.plus(prop: ExtraProperty<D>?): PropertyContainer<D> =
    if (prop == null) this else PropertyContainer(map + (prop.key to prop))


interface WithExtraProperties<C : Any> {
    val extra: PropertyContainer<C>

    fun withNewExtras(newExtras: PropertyContainer<C>): C
}

fun <C> C.mergeExtras(left: C, right: C): C where C : Any, C : WithExtraProperties<C> {
    val aggregatedExtras: List<List<ExtraProperty<C>>> =
        (left.extra.map.values + right.extra.map.values)
            .groupBy { it.key }
            .values
            .map { it.distinct() }

    val (unambiguous, toMerge) = aggregatedExtras.partition { it.size == 1 }

    @Suppress("UNCHECKED_CAST")
    val strategies: List<MergeStrategy<C>> = toMerge.map { (l, r) ->
        (l.key as ExtraProperty.Key<C, ExtraProperty<C>>).mergeStrategyFor(l, r)
    }

    strategies.filterIsInstance<MergeStrategy.Fail>().firstOrNull()?.error?.invoke()

    val replaces: List<ExtraProperty<C>> =
        strategies.filterIsInstance<MergeStrategy.Replace<C>>().map { it.newProperty }

    val needingFullMerge: List<(preMerged: C, left: C, right: C) -> C> =
        strategies.filterIsInstance<MergeStrategy.Full<C>>().map { it.merger }

    val newExtras = PropertyContainer((unambiguous.flatten() + replaces).associateBy { it.key })

    return needingFullMerge.fold(withNewExtras(newExtras)) { acc, merger -> merger(acc, left, right) }
}
