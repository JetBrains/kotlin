/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer

sealed interface TargetDependent<T : Any> {
    fun getOrNull(target: CommonizerTarget): T?
    operator fun get(target: CommonizerTarget): T {
        return getOrNull(target) ?: throw NoSuchElementException("Missing element for target ${target.prettyName}")
    }
}

internal fun <T : Any> Map<CommonizerTarget, T>.toTargetDependent(): TargetDependent<T> {
    return TargetDependent(toMap())
}

internal fun <T : Any> Map<CommonizerTarget, T>.asTargetDependent(): TargetDependent<T> {
    return TargetDependent(this)
}

internal fun <T : Any, R : Any> TargetDependent<T>.map(mapper: (T) -> R): TargetDependent<R> {
    return TargetDependent { target -> this@map.getOrNull(target)?.let(mapper) }
}

internal fun <T : Any> TargetDependent(map: Map<CommonizerTarget, T>): TargetDependent<T> {
    return MapBasedTargetDependent(map)
}

internal fun <T : Any> TargetDependent(factory: (target: CommonizerTarget) -> T?): TargetDependent<T> {
    return FactoryBasedTargetDependent(factory)
}

private class MapBasedTargetDependent<T : Any>(private val map: Map<CommonizerTarget, T>) : TargetDependent<T> {
    override fun getOrNull(target: CommonizerTarget): T? = map[target]
}

private class FactoryBasedTargetDependent<T : Any>(private val factory: (target: CommonizerTarget) -> T?) : TargetDependent<T> {
    private val values = mutableMapOf<CommonizerTarget, Any>()
    override fun getOrNull(target: CommonizerTarget): T? {
        @Suppress("unchecked_cast")
        return values.getOrPut(target) { factory(target) ?: Null }.takeIf { it != Null }?.run { this as T }
    }

    private object Null
}
