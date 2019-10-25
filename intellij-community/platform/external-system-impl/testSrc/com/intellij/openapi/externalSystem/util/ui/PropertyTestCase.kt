// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import com.intellij.openapi.externalSystem.util.properties.GraphProperty
import com.intellij.openapi.externalSystem.util.properties.GraphPropertyImpl
import com.intellij.openapi.externalSystem.util.properties.PropertyGraph
import junit.framework.TestCase
import org.junit.Assert

abstract class PropertyTestCase : TestCase() {
  private lateinit var propertyGraph: PropertyGraph

  override fun setUp() {
    propertyGraph = PropertyGraph()
  }

  protected fun <T> property(initial: () -> T): GraphProperty<T> {
    return GraphPropertyImpl(propertyGraph, initial)
  }

  protected fun <T> assertProperty(property: GraphProperty<T>, value: T, isPropagationBlocked: Boolean) {
    Assert.assertEquals(isPropagationBlocked, propertyGraph.isPropagationBlocked(property))
    Assert.assertEquals(value, property.get())
  }

  protected fun <E> generate(times: Int, generate: (Int) -> E) = (0 until times).map(generate)
}