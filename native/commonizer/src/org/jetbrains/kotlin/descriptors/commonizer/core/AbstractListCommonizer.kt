/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirType

/**
 * Unlike [Commonizer] which commonizes only single elements, this [AbstractListCommonizer] commonizes lists of elements using
 * [Commonizer]s produced by [singleElementCommonizerFactory].
 *
 * Example:
 *   Input: N lists of [CirType]
 *   Output: list of [CirType]
 */
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

        if (commonizers.size != next.size) // lists must be of the same size
            error = true
        else
            for (index in next.indices) {
                val commonizer = commonizers[index]
                val nextElement = next[index]

                // commonize each element in the list:
                if (!commonizer.commonizeWith(nextElement)) {
                    error = true
                    break
                }
            }

        return !error
    }
}
