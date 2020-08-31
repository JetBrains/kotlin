/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.json.utils

internal fun <K, V> Map<K, V>.mergedWith(other: Map<K, V>, merge: V.(other: V?) -> V): Map<K, V> {
    return (keys + other.keys)
        .associateWith { key ->
            val left = this[key]
            val right = other[key]
            if (left != null) {
                left.merge(right)
            } else {
                right!!.merge(left)
            }
        }
}