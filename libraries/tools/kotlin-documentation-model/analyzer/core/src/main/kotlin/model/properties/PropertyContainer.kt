/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.properties

public data class PropertyContainer<C : Any> internal constructor(
    @PublishedApi internal val map: Map<ExtraProperty.Key<C, *>, ExtraProperty<C>>
) {
    public operator fun <D : C> plus(prop: ExtraProperty<D>): PropertyContainer<D> =
        PropertyContainer(map + (prop.key to prop))

    // TODO: Add logic for caching calculated properties
    public inline operator fun <reified T : Any> get(key: ExtraProperty.Key<C, T>): T? = when (val prop = map[key]) {
        is T? -> prop
        else -> throw ClassCastException("Property for $key stored under not matching key type.")
    }

    public inline fun <reified T : Any> allOfType(): List<T> = map.values.filterIsInstance<T>()

    public fun <D : C> addAll(extras: Collection<ExtraProperty<D>>): PropertyContainer<D> =
        PropertyContainer(map + extras.map { p -> p.key to p })

    public operator fun <D : C> minus(prop: ExtraProperty.Key<C, *>): PropertyContainer<D> =
        PropertyContainer(map.filterNot { it.key == prop })

    public companion object {
        public fun <T : Any> empty(): PropertyContainer<T> = PropertyContainer(emptyMap())
        public fun <T : Any> withAll(vararg extras: ExtraProperty<T>?): PropertyContainer<T> = empty<T>().addAll(extras.filterNotNull())
        public fun <T : Any> withAll(extras: Collection<ExtraProperty<T>>): PropertyContainer<T> = empty<T>().addAll(extras)
    }
}

public operator fun <D: Any> PropertyContainer<D>.plus(prop: ExtraProperty<D>?): PropertyContainer<D> =
    if (prop == null) this else PropertyContainer(map + (prop.key to prop))


public interface WithExtraProperties<C : Any> {
    public val extra: PropertyContainer<C>

    public fun withNewExtras(newExtras: PropertyContainer<C>): C
}

public fun <C> C.mergeExtras(left: C, right: C): C where C : Any, C : WithExtraProperties<C> {
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
