/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.group

class LambdaKeyCache {
    private val cache = mutableMapOf<String, Int>()

    operator fun get(descriptor: String): Int? = cache[descriptor]

    operator fun set(descriptor: String, key: Int) {
        cache[descriptor] = key
    }
}