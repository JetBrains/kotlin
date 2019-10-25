// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

open class AtomicLazyProperty<T>(private val initial: () -> T) : ObservableClearableProperty<T> {

  private val value = AtomicReference<Any?>(UNINITIALIZED_VALUE)

  private val changeListeners = CopyOnWriteArrayList<(T) -> Unit>()
  private val resetListeners = CopyOnWriteArrayList<() -> Unit>()

  override fun get(): T {
    @Suppress("UNCHECKED_CAST")
    return value.updateAndGet { if (it === UNINITIALIZED_VALUE) initial() else it } as T
  }

  override fun set(value: T) {
    this.value.set(value)
    changeListeners.forEach { it(value) }
  }

  override fun reset() {
    value.set(UNINITIALIZED_VALUE)
    resetListeners.forEach { it() }
  }

  override fun afterChange(listener: (T) -> Unit) {
    changeListeners.add(listener)
  }

  override fun afterReset(listener: () -> Unit) {
    resetListeners.add(listener)
  }

  companion object {
    private val UNINITIALIZED_VALUE = Any()
  }
}