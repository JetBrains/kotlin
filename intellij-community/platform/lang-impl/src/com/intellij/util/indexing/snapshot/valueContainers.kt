// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot

import com.intellij.util.indexing.ValueContainer

internal class OneRecordValueContainer<V>(private val inputId: Int, private val value: V?): ValueContainer<V>() {
  override fun getValueIterator(): ValueIterator<V> = OneRecordValueIterator(inputId, value)

  override fun size() = 1
}

internal object EmptyValueContainer: ValueContainer<Nothing>() {
  override fun getValueIterator(): ValueIterator<Nothing> = EmptyValueIterator

  override fun size() = 0
}

private class OneRecordValueIterator<V>(private val inputId: Int, private val value: V?): ValueContainer.ValueIterator<V> {
  private var processed = false

  override fun next(): V? {
    assert(!processed)
    processed = true
    return value
  }

  override fun getInputIdsIterator() = OneRecordIntIterator(inputId)

  override fun remove() = throw UnsupportedOperationException("remove is only supported from IndexStorage")

  override fun getValueAssociationPredicate() = ValueContainer.IntPredicate { id -> id == inputId }

  override fun hasNext(): Boolean = !processed
}

private class OneRecordIntIterator(private val inputId: Int): ValueContainer.IntIterator {
  private var processed = false

  override fun hasNext() = !processed

  override fun next(): Int {
    assert(!processed)
    processed = true
    return inputId
  }

  override fun size() = 1
}

private object EmptyValueIterator: ValueContainer.ValueIterator<Nothing> {
  override fun next() = throw IllegalStateException()

  override fun getInputIdsIterator() = EmptyIntIterator

  override fun remove() = throw IllegalStateException()

  override fun getValueAssociationPredicate(): ValueContainer.IntPredicate? = null

  override fun hasNext() = false
}

private object EmptyIntIterator: ValueContainer.IntIterator {
  override fun hasNext() = true

  override fun next() = throw IllegalStateException()

  override fun size() = 0
}