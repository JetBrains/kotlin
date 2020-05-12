// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity

internal class HighlightingProblem(val info: HighlightInfo) : Problem {

  override val description: String
    get() = info.description

  override val severity: Severity
    get() = when (info.severity) {
      HighlightSeverity.ERROR -> Severity.ERROR
      HighlightSeverity.WARNING -> Severity.WARNING
      else -> Severity.INFORMATION
    }

  override val offset: Int
    get() = info.actualStartOffset

  override fun hashCode() = info.hashCode()

  override fun equals(other: Any?) = other is HighlightingProblem && other.info == info
}
