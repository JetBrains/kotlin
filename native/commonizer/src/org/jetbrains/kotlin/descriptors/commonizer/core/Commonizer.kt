/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

interface Commonizer<T, R> {
    val result: R
    fun commonizeWith(next: T): Boolean
}

abstract class AbstractStandardCommonizer<T, R> : Commonizer<T, R> {
    private enum class State {
        EMPTY,
        ERROR,
        IN_PROGRESS
    }

    private var state = State.EMPTY

    final override val result: R
        get() = when (state) {
            State.EMPTY, State.ERROR -> throw IllegalCommonizerStateException()
            State.IN_PROGRESS -> commonizationResult()
        }

    final override fun commonizeWith(next: T): Boolean {
        val result = when (state) {
            State.ERROR -> return false
            State.EMPTY -> {
                initialize(next)
                doCommonizeWith(next)
            }
            State.IN_PROGRESS -> doCommonizeWith(next)
        }

        state = if (!result) State.ERROR else State.IN_PROGRESS

        return result
    }

    protected abstract fun commonizationResult(): R

    protected abstract fun initialize(first: T)
    protected abstract fun doCommonizeWith(next: T): Boolean
}

class IllegalCommonizerStateException : IllegalStateException("Illegal commonizer state: error or empty")
