// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.io.write
import org.assertj.core.api.AbstractStringAssert
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class StringAssertEx(actual: String?) : AbstractStringAssert<StringAssertEx>(actual, StringAssertEx::class.java) {
  fun isEqualTo(expected: Path) {
    isNotNull

    compareFileContent(actual, expected)
  }

  fun toMatchSnapshot(snapshotFile: Path) {
    snapshotFileUsageListeners.forEach { it.beforeMatch(snapshotFile) }

    isNotNull

    val expected = try {
      loadSnapshotContent(snapshotFile)
    }
    catch (e: NoSuchFileException) {
      if (UsefulTestCase.IS_UNDER_TEAMCITY) {
        throw e
      }

      println("Write a new snapshot ${snapshotFile.fileName}")
      snapshotFile.write(actual)
      return
    }

    compareFileContent(actual, expected, snapshotFile)
  }
}