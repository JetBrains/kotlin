/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.utils.CommonizerMap

sealed interface TargetDependent<T> : Iterable<T> {
    val size: Int get() = targets.size
    val targets: List<CommonizerTarget>
    fun indexOf(target: CommonizerTarget): Int = targets.indexOf(target)

    fun <R> map(mapper: (target: CommonizerTarget, T) -> R): TargetDependent<R> {
        return TargetDependent(targets) { target ->
            mapper(target, get(target))
        }
    }

    override fun iterator(): Iterator<T> {
        return iterator { for (key in targets) yield(this@TargetDependent[key]) }
    }

    operator fun get(target: CommonizerTarget): T

    fun getOrNull(target: CommonizerTarget): T? {
        return if (target in targets) get(target) else null
    }

    operator fun get(index: Int): T = get(targets[index])

    fun getOrNull(index: Int): T? {
        return getOrNull(targets.getOrNull(index) ?: return null)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): TargetDependent<T> = Empty as TargetDependent<T>
    }

    object Empty : TargetDependent<Any?> {
        override val targets: List<CommonizerTarget> = emptyList()
        override fun get(target: CommonizerTarget) = throwMissingTarget(target)
    }
}

internal fun <T : Any> TargetDependent<T?>.filterNonNull(): TargetDependent<T> {
    val nonNullTargets = targets.filter { this[it] != null }
    return TargetDependent(nonNullTargets) { target -> this@filterNonNull[target] ?: throw NullPointerException() }
}

internal fun <T> TargetDependent<T>.toMap(): Map<CommonizerTarget, T> {
    return mutableMapOf<CommonizerTarget, T>().apply {
        for (target in targets) {
            put(target, this@toMap[target])
        }
    }
}

internal fun <T> Map<out CommonizerTarget, T>.toTargetDependent(): TargetDependent<T> {
    return TargetDependent(toMap())
}

internal fun <T, R> TargetDependent<T>.mapValue(mapper: (T) -> R): TargetDependent<R> {
    return TargetDependent(targets) { target -> this@mapValue.get(target).let(mapper) }
}

internal inline fun <T, R> TargetDependent<T>.mapValueEager(mapper: (T) -> R): TargetDependent<R> {
    return EagerTargetDependent(targets) { target -> mapper(this[target]) }
}

internal inline fun <T, R> TargetDependent<T>.mapValueEagerWithTarget(mapper: (target: CommonizerTarget, T) -> R): TargetDependent<R> {
    return EagerTargetDependent(targets) { target -> mapper(target, this[target]) }
}

internal fun <T, R> TargetDependent<T>.mapTargets(mapper: (CommonizerTarget) -> R): TargetDependent<R> {
    return TargetDependent(targets) { target -> mapper(target) }
}

internal inline fun <T> TargetDependent<T>.forEachWithTarget(action: (target: CommonizerTarget, T) -> Unit) {
    targets.forEach { target -> action(target, this[target]) }
}

internal fun <T> TargetDependent(map: Map<out CommonizerTarget, T>): TargetDependent<T> {
    return MapBasedTargetDependent(map.toMap())
}

internal fun <T> TargetDependent(keys: Iterable<CommonizerTarget>, factory: (target: CommonizerTarget) -> T): TargetDependent<T> {
    return FactoryBasedTargetDependent(keys.toList(), factory)
}

internal fun <T> TargetDependent(vararg pairs: Pair<CommonizerTarget, T>) = pairs.toMap().toTargetDependent()

internal inline fun <T> EagerTargetDependent(
    keys: Iterable<CommonizerTarget>, factory: (target: CommonizerTarget) -> T
): TargetDependent<T> {
    return keys.associateWith(factory).toTargetDependent()
}

private class MapBasedTargetDependent<T>(private val map: Map<CommonizerTarget, T>) : TargetDependent<T> {
    override val targets: List<CommonizerTarget> = map.keys.toList()
    override fun get(target: CommonizerTarget): T = map.getValue(target)
}

/**
 * Not thread safe!
 */
private class FactoryBasedTargetDependent<T>(
    override val targets: List<CommonizerTarget>,
    private var factory: ((target: CommonizerTarget) -> T)?
) : TargetDependent<T> {

    private object Null
    private object Uninitialized

    private val values = targets.associateWithTo(CommonizerMap<CommonizerTarget, Any>(targets.size)) { Uninitialized }

    @Suppress("UNCHECKED_CAST")
    override fun get(target: CommonizerTarget): T {
        val value = values[target] ?: throwMissingTarget(target)
        if (value === Null) return null as T
        if (value === Uninitialized) {
            val initializedValue = checkNotNull(factory)(target)
            values[target] = initializedValue ?: Null
            if (values.none { it.value !== Uninitialized }) {
                factory = null
            }
            return initializedValue
        }
        return value as T
    }
}

private fun throwMissingTarget(target: CommonizerTarget): Nothing = throw NoSuchElementException("Missing target $target")
