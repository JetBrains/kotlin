// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

interface GraphProperty<T> : ObservableClearableProperty<T> {
  fun dependsOn(parent: ObservableClearableProperty<*>, default: () -> T)

  fun afterPropagation(listener: () -> Unit)
}