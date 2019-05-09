// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.SystemProperties
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.*
import gnu.trove.THashSet
import junit.framework.ComparisonFailure
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.PathAssert
import org.assertj.core.internal.ComparatorBasedComparisonStrategy
import org.assertj.core.internal.Iterables
import org.junit.rules.ExternalResource
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*

class PathAssertEx(actual: Path?) : PathAssert(actual) {
  override fun doesNotExist(): PathAssert {
    isNotNull

    if (Files.exists(actual, LinkOption.NOFOLLOW_LINKS)) {
      var error = "Expecting path:\n\t${actual}\nnot to exist"
      if (actual.size() < 16 * 1024) {
        error += ", content:\n\n${actual.readText()}\n"
      }
      failWithMessage(error)
    }

    return this
  }

  override fun hasContent(expected: String) = isEqualTo(expected)

  fun isEqualTo(expected: String): PathAssertEx {
    isNotNull
    isRegularFile

    val expectedContent = expected.trimIndent()
    val actualContent = StringUtilRt.convertLineSeparators(actual.readText())
    if (actualContent != expectedContent) {
      throw ComparisonFailure(null, expectedContent, actualContent)
    }

    return this
  }

  fun hasChildren(vararg names: String) {
    paths.assertIsDirectory(info, actual)

    Iterables(ComparatorBasedComparisonStrategy(Comparator<Any> { o1, o2 ->
      if (o1 is Path && o2 is Path) {
        o1.compareTo(o2)
      }
      else if (o1 is String && o2 is String) {
        o1.compareTo(o2)
      }
      else if (o1 is String) {
        if ((o2 as Path).endsWith(o1)) 0 else -1
      }
      else {
        if ((o1 as Path).endsWith(o2 as String)) 0 else -1
      }
    }))
      .assertContainsOnly(info, Files.newDirectoryStream(actual).use { it.toList() }, names)
  }
}

private interface SnapshotFileUsageListener {
  fun active(file: Path)
}

private val snapshotFileUsageListeners = ContainerUtil.newConcurrentSet<SnapshotFileUsageListener>()

class StringAssertEx(actual: String?) : AbstractStringAssert<StringAssertEx>(actual, StringAssertEx::class.java) {
  fun isEqualTo(expected: Path) {
    isNotNull

    compareFileContent(actual, expected)
  }

  fun toMatchSnapshot(snapshotFile: Path) {
    snapshotFileUsageListeners.forEach { it.active(snapshotFile) }

    isNotNull

    try {
      compareFileContent(actual, snapshotFile,
                         updateIfMismatch = !UsefulTestCase.IS_UNDER_TEAMCITY && SystemProperties.getBooleanProperty("test.update.snapshots", false))
    }
    catch (e: NoSuchFileException) {
      if (UsefulTestCase.IS_UNDER_TEAMCITY) {
        throw e
      }

      println("Write a new snapshot ${snapshotFile.fileName}")
      snapshotFile.write(actual)
    }
  }
}

class CleanupSnapshots(private val dir: Path) : ExternalResource() {
  private val usedPaths: MutableSet<Path> = THashSet<Path>()

  private val listener = object : SnapshotFileUsageListener {
    override fun active(file: Path) {
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