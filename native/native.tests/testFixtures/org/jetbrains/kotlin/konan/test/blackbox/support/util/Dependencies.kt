/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun <N : Any> StorageManager.lazyNeighbors(
    directNeighbors: () -> Set<N>,
    computeNeighbors: (N) -> Set<N>,
    describe: (N) -> String = { it.toString() }
): ReadOnlyProperty<N, Set<N>> = object : ReadOnlyProperty<N, Set<N>> {
    private val lazyValue: NotNullLazyValue<Set<N>> = this@lazyNeighbors.createLazyValue(
        computable = {
            val neighbors = directNeighbors()
            if (neighbors.isEmpty())
                emptySet()
            else
                hashSetOf<N>().apply {
                    addAll(neighbors)
                    neighbors.forEach { neighbor -> addAll(computeNeighbors(neighbor)) }
                }
        },
        onRecursiveCall = { throw CyclicNeighborsException() })

    override fun getValue(thisRef: N, property: KProperty<*>): Set<N> = try {
        lazyValue.invoke()
    } catch (e: CyclicNeighborsException) {
        throw e.trace("Property ${property.name} of ${describe(thisRef)}")
    }
}

private class CyclicNeighborsException : Exception() {
    private val backtrace = mutableListOf<String>()

    fun trace(element: String) = apply { backtrace += element }

    override val message
        get() = buildString {
            appendLine("Cyclic neighbors detected. Backtrace (${backtrace.size} elements):")
            backtrace.joinTo(this, separator = "\n")
        }
}
