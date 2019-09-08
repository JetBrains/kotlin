/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

abstract class AbstractListCommonizer<T, R>(
    private val singleElementCommonizerFactory: () -> Commonizer<T, R>
) : Commonizer<List<T>, List<R>> {
    private var commonizers: List<Commonizer<T, R>>? = null
    private var error = false

    final override val result: List<R>
        get() = commonizers?.takeIf { !error }?.map { it.result } ?: throw IllegalCommonizerStateException()

    final override fun commonizeWith(next: List<T>): Boolean {
        if (error)
            return false

        val commonizers = commonizers
            ?: mutableListOf<Commonizer<T, R>>().apply {
                repeat(next.size) {
                    this += singleElementCommonizerFactory()
                }
            }.also {
                this.commonizers = it
            }

        if (commonizers.size != next.size)
            error = true
        else
            for (index in 0 until next.size) {
                val commonizer = commonizers[index]
                val nextElement = next[index]

                if (!commonizer.commonizeWith(nextElement)) {
                    error = true
                    break
                }
            }

        return !error
    }
}
