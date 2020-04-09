// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport.update

import com.intellij.util.ui.update.Update

internal abstract class PriorityEatUpdate(private val priority: Long) : Update(Any()) {
  override fun canEat(update: Update): Boolean {
    if (update !is PriorityEatUpdate) return false
    return priority <= update.priority
  }

  companion object {
    operator fun invoke(priority: Long, update: () -> Unit) =
      object : PriorityEatUpdate(priority) {
        override fun run() = update()
      }
  }
}