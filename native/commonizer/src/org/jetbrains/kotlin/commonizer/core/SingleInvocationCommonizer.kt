/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

interface SingleInvocationCommonizer<T : Any> {
    operator fun invoke(values: List<T>): T
}

fun <T : Any> SingleInvocationCommonizer<T>.asCommonizer(): Commonizer<T, T> = object : Commonizer<T, T> {
    private val collectedValues = mutableListOf<T>()

    override val result: T
        get() = this@asCommonizer.invoke(collectedValues)

    override fun commonizeWith(next: T): Boolean {
        collectedValues.add(next)
        return true
    }
}