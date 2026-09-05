/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
public inline fun <K, V : Any> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    return associateWith { valueSelector(it) }.filterValues { it != null } as Map<K, V>
}
