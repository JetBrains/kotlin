// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.io.delete
import com.intellij.util.io.directoryStreamIfExists
import com.intellij.util.io.isFile
import com.intellij.util.io.isHidden
import gnu.trove.THashSet
import org.junit.rules.ExternalResource
import java.nio.file.Path

class CleanupSnapshots(private val dir: Path) : ExternalResource() {
  private val usedPaths: MutableSet<Path> = THashSet<Path>()

  private val listener = object : SnapshotFileUsageListener {
    override fun beforeMatch(file: Path) {
      if (file.startsWith(dir)) {
        usedPaths.add(file)
      }
    }
  }

  override fun before() {
    if (!UsefulTestCase.IS_UNDER_TEAMCITY) {
      snapshotFileUsageListeners.add(listener)
    }
  }

  override fun after() {
    dir.directoryStreamIfExists {
      for (file in it) {
        if (!usedPaths.contains(file) && !file.isHidden() && file.isFile()) {
          file.delete(false)
          println("Remove outdated snapshot ${dir.relativize(file)}")
        }
      }
    }
    snapshotFileUsageListeners.remove(listener)
  }
}