// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

open class GraphPropertyImpl<T>(private val propertyGraph: PropertyGraph, initial: () -> T)
  : GraphProperty<T>, AtomicLazyProperty<T>(initial) {

  override fun dependsOn(parent: ObservableClearableProperty<*>, default: () -> T) {
    propertyGraph.dependsOn(this, parent, default)
  }

  override fun afterPropagation(listener: () -> Unit) {
    propertyGraph.afterPropagation(listener)
  }

  init {
    propertyGraph.register(this)
  }
}