/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

class FreshEntityProducer<R>(private val build: (Int) -> R) {
    private var next = 0
    fun getFresh(): R = build(next++)
}