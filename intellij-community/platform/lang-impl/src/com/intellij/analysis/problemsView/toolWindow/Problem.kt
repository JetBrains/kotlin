// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import javax.swing.Icon

interface Problem {
  val icon: Icon
  val description: String
  val severity: Int
  val offset: Int
}
