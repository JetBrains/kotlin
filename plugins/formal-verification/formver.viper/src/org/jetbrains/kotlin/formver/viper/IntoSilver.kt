/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import scala.Option

interface IntoSilver<out T> {
    fun toSilver(): T
}

fun <T, V> Option<T>.toSilver(): Option<V> where T : IntoSilver<V> = map { it.toSilver() }

fun <T, V> List<T>.toSilver(): List<V> where T : IntoSilver<V> =
    map { it.toSilver() }

fun <K, V, K2, V2> Map<K, V>.toSilver(): Map<K2, V2> where K : IntoSilver<K2>, V : IntoSilver<V2> =
    this.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }