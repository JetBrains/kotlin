// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class AtomicBooleanProperty(initial: Boolean) : BooleanProperty {

  private val value = AtomicBoolean(initial)

  private val changeListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
  private val setListeners = CopyOnWriteArrayList<() -> Unit>()
  private val resetListeners = CopyOnWriteArrayList<() -> Unit>()

  override fun set() = set(true)

  override fun reset() = set(false)

  override fun set(value: Boolean) {
    val oldValue = this.value.getAndSet(value)
    when {
      !oldValue && value -> setListeners.forEach { it() }
      oldValue && !value -> resetListeners.forEach { it() }
    }
    changeListeners.forEach { it(value) }
  }

  override fun get() = value.get()

  override fun afterChange(listener: (Boolean) -> Unit) {
    changeListeners.add(listener)
  }

  override fun afterSet(listener: () -> Unit) {
    setListeners.add(listener)
  }

  override fun afterReset(listener: () -> Unit) {
    resetListeners.add(listener)
  }
}