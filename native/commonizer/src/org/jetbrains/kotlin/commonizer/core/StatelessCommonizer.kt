/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

interface StatelessCommonizer<T, R> {
    fun commonize(values: List<T>): R?
}

open class StatelessCommonizerAdapter<T, R : Any>(
    private val commonizer: StatelessCommonizer<T, R>
) : AbstractStandardCommonizer<T, R>(), StatelessCommonizer<T, R> by commonizer {
    private val values = mutableListOf<T>()

    override fun commonizationResult(): R {
        try {
            return checkNotNull(commonize(values.toList()))
        } finally {
            values.clear()
        }
    }

    override fun initialize(first: T) = Unit

    override fun doCommonizeWith(next: T): Boolean {
        values.add(next)
        return commonize(values.toList()) != null
    }
}
