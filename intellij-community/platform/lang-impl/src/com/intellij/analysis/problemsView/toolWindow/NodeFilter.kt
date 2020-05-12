// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import java.util.function.Predicate

internal data class NodeFilter(
  private val showErrors: Boolean,
  private val showWarnings: Boolean,
  private val showInformation: Boolean)
  : Predicate<Node?> {

  override fun test(node: Node?): Boolean {
    return if (node !is ProblemNode) true
    else when (node.problem.severity) {
      Severity.ERROR -> showErrors
      Severity.WARNING -> showWarnings
      Severity.INFORMATION -> showInformation
    }
  }
}
