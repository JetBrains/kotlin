// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import gnu.trove.THashMap

class ProjectResolverInternary {
  private var data = THashMap<Any, Any>()

  @Suppress("UNCHECKED_CAST")
  @JvmOverloads
  fun <T> intern(value: T, transform: (T) -> T = { it }): T {
    if (value == null) return value
    return data.getOrPut(value, { transform(value) }) as T
  }
}
