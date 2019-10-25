// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface ObservableClearableProperty<T> : ReadWriteProperty<Any?, T> {
  fun get(): T

  fun set(value: T)

  fun reset()

  fun afterChange(listener: (T) -> Unit)

  fun afterReset(listener: () -> Unit)

  override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}