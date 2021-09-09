/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

interface NullableContextualSingleInvocationCommonizer<T, R : Any> {
    operator fun invoke(values: List<T>): R?
}

fun <T, R : Any> NullableContextualSingleInvocationCommonizer<T, R>.asCommonizer(): Commonizer<T, R?> =
    object : Commonizer<T, R?> {
        private val collectedValues = mutableListOf<T>()

        override val result: R?
            get() = this@asCommonizer.invoke(collectedValues)

        override fun commonizeWith(next: T): Boolean {
            collectedValues.add(next)
            return true
        }
    }
