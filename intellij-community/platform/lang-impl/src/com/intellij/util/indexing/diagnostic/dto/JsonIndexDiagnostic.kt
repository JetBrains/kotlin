// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

import com.intellij.util.indexing.diagnostic.ProjectIndexingHistory

data class JsonIndexDiagnostic(
  val appInfo: JsonIndexDiagnosticAppInfo,
  val runtimeInfo: JsonRuntimeInfo,
  val projectIndexingHistory: JsonProjectIndexingHistory
) {
  companion object {
    fun generateForHistory(projectIndexingHistory: ProjectIndexingHistory): JsonIndexDiagnostic =
      JsonIndexDiagnostic(
        JsonIndexDiagnosticAppInfo.create(),
        JsonRuntimeInfo.create(),
        projectIndexingHistory.toJson()
      )
  }
}