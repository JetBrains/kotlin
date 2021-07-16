/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal external interface Es6Iterator<E> {
    fun next(): NextResult<E>

    interface NextResult<E> {
        val done: Boolean // | undefined
        val value: E
    }
}

internal open class Es6IteratorAdapter<T>(
    private val iter: Es6Iterator<T>
) : Iterator<T> {
    protected var current = iter.next()
        private set

    override fun hasNext(): Boolean {
        return current.done != true
    }

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        val value = current.value
        current = iter.next()
        return value
    }
}

internal open class Es6IteratorAdapterWithLast<T>(iter: Es6Iterator<T>) : Es6IteratorAdapter<T>(iter) {
    protected var lastValue: T? = null
        private set

    override fun next(): T {
        return super.next().also {
            lastValue = it
        }
    }
}