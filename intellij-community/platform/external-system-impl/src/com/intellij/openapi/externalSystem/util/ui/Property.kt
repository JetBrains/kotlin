// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Property<T>(initial: () -> T) : ReadWriteProperty<Any?, T> {
  private var isModified = false
  private var isUpdating = false

  private var fieldProperty = field(initial)
  private var field: T by fieldProperty

  private val listeners = ArrayList<(T) -> Unit>()

  private val childProperties = ArrayList<Property<*>>()
  private val parentProperties = LinkedHashMap<Property<*>, () -> T>()

  fun isModified() = isModified

  fun dependsOn(property: Property<*>, default: () -> T) {
    property.childProperties.add(this)
    parentProperties[property] = default
  }

  fun get(): T = field

  fun set(value: T) {
    updating {
      isModified = true
      field = value
      childProperties.forEach { it.update(this) }
    }
  }

  fun addListener(listener: (T) -> Unit) {
    listeners.add(listener)
  }

  private fun update(property: Property<*>) {
    updating {
      if (isModified) return
      val initial = parentProperties[property] ?: return
      val value = initial.invoke()
      field = value
      childProperties.forEach { it.update(this) }
      firePropertyChanged(value)
    }
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

  private fun firePropertyChanged(value: T) {
    invokeLater {
      updating {
        listeners.forEach { it(value) }
      }
    }
  }

  private inline fun updating(updateAction: () -> Unit) {
    assertIsExecutionThread()
    if (isUpdating) return
    isUpdating = true
    try {
      updateAction()
    }
    finally {
      isUpdating = false
    }
  }

  abstract fun assertIsExecutionThread()

  abstract fun invokeLater(action: () -> Unit)

  private fun <T> field(initializer: () -> T) = Field(initializer)

  class Field<T>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {
    private var field = Optional.empty<T>()
    private var initialField = Optional.empty<T>()

    private val initial: T
      get() {
        if (!initialField.isPresent) {
          initialField = Optional.of(initializer())
        }
        return initialField.get()
      }

    fun get(): T {
      if (!field.isPresent) {
        set(initial)
      }
      return field.get()
    }

    fun set(value: T) {
      field = Optional.of(value)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
  }

  class Optional<out T> private constructor(val isPresent: Boolean, private val value: Any?) {

    fun get(): T {
      @Suppress("UNCHECKED_CAST")
      if (isPresent) return value as T
      throw NoSuchElementException("No value present")
    }

    companion object {
      val EMPTY = Optional<Nothing>(false, null)

      fun <T> empty() = EMPTY as Optional<T>
      fun <T> of(value: T) = Optional<T>(true, value)
    }
  }

  class PropertyView<R, S, T>(
    private val instance: ReadWriteProperty<R, S>,
    private val map: (S) -> T,
    private val coamp: (T) -> S
  ) : ReadWriteProperty<R, T> {

    override fun getValue(thisRef: R, property: KProperty<*>): T {
      return map(instance.getValue(thisRef, property))
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
      instance.setValue(thisRef, property, coamp(value))
    }
  }
}