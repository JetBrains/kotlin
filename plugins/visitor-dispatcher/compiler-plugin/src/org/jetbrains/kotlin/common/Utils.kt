/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.common

fun <K1, K2, V> MutableMap<K1, MutableMap<K2, V>>.getOrPutEmpty(k1: K1): MutableMap<K2, V> {
    return getOrPut(k1) { mutableMapOf() }
}