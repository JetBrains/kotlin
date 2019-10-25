// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

  companion object {
    fun <R, T> ReadWriteProperty<R, T>.map(transform: (T) -> T) = PropertyView(this, transform, { it })

    fun <R, T> ReadWriteProperty<R, T>.comap(transform: (T) -> T) = PropertyView(this, { it }, transform)
  }
}