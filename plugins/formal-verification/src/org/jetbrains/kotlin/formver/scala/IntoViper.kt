/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala

interface IntoViper<out T> {
    fun toViper(): T
}

fun <T, V> List<T>.toViper(): List<V> where T : IntoViper<V> =
    map { it.toViper() }

fun <K, V, K2, V2> Map<K, V>.toViper(): Map<K2, V2> where K : IntoViper<K2>, V : IntoViper<V2> =
    this.mapKeys { it.key.toViper() }.mapValues { it.value.toViper() }