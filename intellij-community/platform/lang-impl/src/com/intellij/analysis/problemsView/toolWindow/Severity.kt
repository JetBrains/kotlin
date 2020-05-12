// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.icons.AllIcons.General
import javax.swing.Icon

enum class Severity(val icon: Icon) {
  ERROR(General.Error),
  WARNING(General.Warning),
  INFORMATION(General.Information),
}
