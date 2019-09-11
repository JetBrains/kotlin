// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BooleanProperty(private var field: Boolean) : ReadWriteProperty<Any?, Boolean> {

  private val setListeners = ArrayList<() -> Unit>()
  private val resetListeners = ArrayList<() -> Unit>()

  fun set() = set(true)

  fun reset() = set(false)

  fun set(value: Boolean) {
    synchronized(this) {
      val oldValue = field
      field = value
      when {
        !oldValue && value -> setListeners.forEach { it() }
        oldValue && !value -> resetListeners.forEach { it() }
      }
    }
  }

  fun get(): Boolean {
    synchronized(this) {
      return field
    }
  }

  fun afterSet(listener: Listener) {
    afterSet(listener::listen)
  }

  fun afterSet(listener: () -> Unit) {
    synchronized(this) {
      setListeners.add(listener)
    }
  }

  fun afterReset(listener: Listener) {
    afterReset(listener::listen)
  }

  fun afterReset(listener: () -> Unit) {
    synchronized(this) {
      resetListeners.add(listener)
    }
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>) = get()

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = set(value)

  interface Listener {
    fun listen()
  }
}