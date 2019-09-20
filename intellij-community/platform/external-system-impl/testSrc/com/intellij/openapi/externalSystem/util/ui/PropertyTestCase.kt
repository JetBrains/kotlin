// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import junit.framework.TestCase
import org.junit.Assert

abstract class PropertyTestCase : TestCase() {

  private lateinit var propertyFactory: MockPropertyFactory

  override fun setUp() {
    propertyFactory = MockPropertyFactory()
  }

  override fun tearDown() {
    propertyFactory.dispose()
  }

  protected fun <T> property(initial: () -> T): Property<T> {
    return propertyFactory.property(initial)
  }

  protected fun <T> Property<T>.setSynchronously(value: T) {
    invokeAndWaitAll {
      set(value)
    }
  }

  protected fun invokeLater(action: () -> Unit) {
    propertyFactory.invokeLater(action)
  }

  protected fun waitAll() {
    propertyFactory.waitAll()
  }

  protected fun invokeAndWaitAll(action: () -> Unit) {
    invokeLater(action)
    waitAll()
  }

  protected fun <T> assertProperty(property: Property<T>, value: T, modified: Boolean) {
    Assert.assertEquals(value, property.get())
    Assert.assertEquals(modified, property.isModified())
  }

  protected fun <E> generate(times: Int, generate: (Int) -> E) = (0 until times).map(generate)
}