// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml


sealed class MLFeatureValue

class BinaryValue private constructor(val value: Boolean) : MLFeatureValue() {
  companion object {
    val TRUE = BinaryValue(true)
    val FALSE = BinaryValue(false)

    fun of(value: Boolean): BinaryValue = if (value) TRUE else FALSE
  }

  override fun toString(): String {
    return if (value) "1" else "0"
  }
}

class FloatValue private constructor(val value: Double) : MLFeatureValue() {
  companion object {
    fun of(value: Double) = FloatValue(value)
    fun of(value: Int) = FloatValue(value.toDouble())
  }

  override fun toString(): String = value.toString()
}

class CategoricalValue<T : Enum<*>> private constructor(val value: T) : MLFeatureValue() {
  companion object {
    fun <T : Enum<*>> of(value: T) = CategoricalValue(value)
  }

  override fun toString(): String = value.toString()
}