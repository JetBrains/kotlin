/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

import java.util.AbstractList

private open class ReversedListReadOnly<T>(protected open val delegate: List<T>) : AbstractList<T>() {
    override val size: Int get() = delegate.size
    override fun get(index: Int): T = delegate[index.flipIndex()]

    protected fun Int.flipIndex(): Int = if (this in 0..size - 1) size - this - 1 else throw IndexOutOfBoundsException("index $this should be in range [${0..size - 1}].")
    protected fun Int.flipIndexForward(): Int = if (this in 0..size) size - this else throw IndexOutOfBoundsException("index $this should be in range [${0..size}].")
}

private class ReversedList<T>(protected override val delegate: MutableList<T>) : ReversedListReadOnly<T>(delegate) {
    override fun clear() = delegate.clear()
    override fun removeAt(index: Int): T = delegate.removeAt(index.flipIndex())

    override fun set(index: Int, element: T): T = delegate.set(index.flipIndex(), element)
    override fun add(index: Int, element: T) {
        delegate.add(index.flipIndexForward(), element)
    }
}

/**
 * Returns a reversed read-only view of the original List.
 * All changes made in the original list will be reflected in the reversed one.
 */
public fun <T> List<T>.asReversed(): List<T> = ReversedListReadOnly(this)

/**
 * Returns a reversed mutable view of the original mutable List.
 * All changes made in the original list will be reflected in the reversed one and vice versa.
 */
@kotlin.jvm.JvmName("asReversedMutable")
public fun <T> MutableList<T>.asReversed(): MutableList<T> = ReversedList(this)

