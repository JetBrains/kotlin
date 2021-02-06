/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.fir.caches

import org.jetbrains.kotlin.fir.caches.FirCache
import java.util.concurrent.ConcurrentHashMap

internal class FirThreadSafeCache<K : Any, V, CONTEXT>(
    private val createValue: (K, CONTEXT) -> V
) : FirCache<K, V, CONTEXT>() {
    private val map = ConcurrentHashMap<K, Any>()

    override fun getValue(key: K, context: CONTEXT): V =
        map.computeIfAbsentWithNullableValue(key) { createValue(it, context) }

    override fun getValueIfComputed(key: K): V? =
        map[key]?.nullValueToNull()
}
