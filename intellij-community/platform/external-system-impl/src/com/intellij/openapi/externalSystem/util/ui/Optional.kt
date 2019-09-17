// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

class Optional<out T> private constructor(val isPresent: Boolean, private val value: Any?) {

  fun get(): T {
    @Suppress("UNCHECKED_CAST")
    if (isPresent) return value as T
    throw NoSuchElementException("No value present")
  }

  fun getOrNull() = if (isPresent) get() else null

  companion object {
    val EMPTY = Optional<Nothing>(false, null)

    fun <T> empty() = EMPTY as Optional<T>
    fun <T> of(value: T) = Optional<T>(true, value)
  }
}