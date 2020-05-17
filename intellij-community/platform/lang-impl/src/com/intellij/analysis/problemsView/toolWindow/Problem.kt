// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.AnAction

interface Problem {
  val description: String
  val severity: Severity
  val offset: Int

  fun hasQuickFixActions() = false
  fun getQuickFixActions(): Collection<AnAction> = emptyList()
}
