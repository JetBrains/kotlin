/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

class FreshEntityProducer<R, S>(private val build: (Int, S) -> R) {
    private var next = 0
    fun getFresh(s: S): R = build(next++, s)
}

typealias SimpleFreshEntityProducer<R> = FreshEntityProducer<R, Unit>

fun <R> simpleFreshEntityProducer(build: (Int) -> R): SimpleFreshEntityProducer<R> = FreshEntityProducer { n, _ -> build(n) }
fun <R> SimpleFreshEntityProducer<R>.getFresh() = getFresh(Unit)

fun indexProducer(): SimpleFreshEntityProducer<Int> = simpleFreshEntityProducer { it }