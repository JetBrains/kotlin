// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.readChars
import com.intellij.util.io.readText
import com.intellij.util.io.size
import junit.framework.ComparisonFailure
import org.assertj.core.api.PathAssert
import org.assertj.core.internal.ComparatorBasedComparisonStrategy
import org.assertj.core.internal.Iterables
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.*

class PathAssertEx(actual: Path?) : PathAssert(actual) {
  override fun doesNotExist(): PathAssert {
    isNotNull

    if (Files.exists(actual, LinkOption.NOFOLLOW_LINKS)) {
      var error = "Expecting path:\n\t$actual\nnot to exist"
      if (actual.size() < 16 * 1024) {
        error += " but it does, with content:\n\n${actual.readText()}\n"
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
    val actualContent = getNormalizedActualContent(actual.readChars())
    if (!StringUtil.equal(expectedContent, actualContent, true)) {
      throw ComparisonFailure(null, expectedContent, actualContent.toString())
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

