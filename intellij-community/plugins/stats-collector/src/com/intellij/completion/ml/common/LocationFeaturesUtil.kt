// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

object LocationFeaturesUtil {
  fun indentLevel(line: String, tabSize: Int): Int {
    if (tabSize <= 0) return 0

    var indentLevel = 0
    var spaces = 0
    for (ch in line) {
      if (spaces == tabSize) {
        indentLevel += 1
        spaces = 0
      }

      if (ch == '\t') {
        indentLevel += 1
        spaces = 0
      }
      else if (ch == ' ') {
        spaces += 1
      }
      else {
        break
      }
    }

    return indentLevel
  }
}