/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

/**
 * A wrapper around [Commonizer] that checks that
 * - either all commonized elements are null
 * - or all commonized elements are non-null and can be commonized according to the wrapped commonized
 */
abstract class AbstractNullableCommonizer<T : Any, R : Any, WT, WR>(
    private val wrappedCommonizerFactory: () -> Commonizer<WT, WR>,
    private val extractor: (T) -> WT,
    private val builder: (WR) -> R
) : Commonizer<T?, R?> {
    private enum class State {
        EMPTY,
        ERROR,
        WITH_WRAPPED,
        WITHOUT_WRAPPED
    }

    private var state = State.EMPTY
    private lateinit var wrapped: Commonizer<WT, WR>

    final override val result: R?
        get() = when (state) {
            State.EMPTY -> failInEmptyState()
            State.ERROR -> failInErrorState()
            State.WITH_WRAPPED -> builder(wrapped.result)
            State.WITHOUT_WRAPPED -> null // null means there is no commonized result
        }

    final override fun commonizeWith(next: T?): Boolean {
        state = when (state) {
            State.ERROR -> return false
            State.EMPTY -> next?.let {
                wrapped = wrappedCommonizerFactory()
                doCommonizeWith(next)
            } ?: State.WITHOUT_WRAPPED
            State.WITH_WRAPPED -> next?.let(::doCommonizeWith) ?: State.ERROR
            State.WITHOUT_WRAPPED -> next?.let { State.ERROR } ?: State.WITHOUT_WRAPPED
        }

        return state != State.ERROR
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun doCommonizeWith(next: T) =
        if (wrapped.commonizeWith(extractor(next))) State.WITH_WRAPPED else State.ERROR
}
