/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty


internal class ThreadLocalValue<V : Any>(private val init: () -> V) {
    private val map = ConcurrentHashMap<Long, V>()

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): V =
        map.computeIfAbsent(Thread.currentThread().id) {
            init()
        }
}

internal fun <V : Any> threadLocal(init: () -> V): ThreadLocalValue<V> =
    ThreadLocalValue(init)
