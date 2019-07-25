// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import org.assertj.core.api.AbstractStringAssert
import java.nio.file.Path

class StringAssertEx(actual: String?) : AbstractStringAssert<StringAssertEx>(actual, StringAssertEx::class.java) {
  fun toMatchSnapshot(snapshotFile: Path) {
    snapshotFileUsageListeners.forEach { it.beforeMatch(snapshotFile) }
    isNotNull
    compareFileContent(actual, snapshotFile)
  }
}