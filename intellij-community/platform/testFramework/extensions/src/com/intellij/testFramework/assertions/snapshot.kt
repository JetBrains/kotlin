// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.readChars
import com.intellij.util.io.write
import org.assertj.core.api.ListAssert
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer
import java.nio.file.Path
import java.util.regex.Pattern

internal interface SnapshotFileUsageListener {
  fun beforeMatch(file: Path)
}

internal val snapshotFileUsageListeners = ContainerUtil.newConcurrentSet<SnapshotFileUsageListener>()

class ListAssertEx<ELEMENT>(actual: List<ELEMENT>) : ListAssert<ELEMENT>(actual) {
  fun toMatchSnapshot(snapshotFile: Path) {
    isNotNull

    compareFileContent(actual, snapshotFile)
  }
}

fun dumpData(data: Any): String {
  val dumperOptions = DumperOptions()
  dumperOptions.isAllowReadOnlyProperties = true
  dumperOptions.lineBreak = DumperOptions.LineBreak.UNIX
  val yaml = Yaml(DumpRepresenter(), dumperOptions)
  return yaml.dump(data)
}

private class DumpRepresenter : Representer() {
  init {
    representers.put(Pattern::class.java, RepresentDump())
  }

  private inner class RepresentDump : Represent {
    override fun representData(data: Any): Node = representScalar(Tag.STR, data.toString())
  }
}

@Throws(FileComparisonFailure::class)
fun compareFileContent(actual: Any, snapshotFile: Path, updateIfMismatch: Boolean = false) {
  compareFileContent(actual, loadSnapshotContent(snapshotFile), snapshotFile, updateIfMismatch)
}

internal fun loadSnapshotContent(snapshotFile: Path, convertLineSeparators: Boolean = SystemInfoRt.isWindows): CharSequence {
  // because developer can open file and depending on editor settings, newline maybe added to the end of file
  var content = snapshotFile.readChars().trimEnd()
  if (convertLineSeparators) {
    content = StringUtilRt.convertLineSeparators(content, "\n")
  }
  return content
}

@Throws(FileComparisonFailure::class)
internal fun compareFileContent(actual: Any, expected: CharSequence, snapshotFile: Path, updateIfMismatch: Boolean = isUpdateSnapshotIfMismatch()) {
  val actualContent = if (actual is CharSequence) actual.trimEnd() else dumpData(actual).trimEnd()
  if (StringUtil.equal(actualContent, expected, true)) {
    return
  }

  if (updateIfMismatch) {
    System.out.println("UPDATED snapshot ${snapshotFile.fileName}")
    snapshotFile.write(actualContent.toString())
  }
  else {
    @Suppress("SpellCheckingInspection")
    throw FileComparisonFailure(
      "Received value does not match stored snapshot ${snapshotFile.fileName}.\nInspect your code changes or run with `-Dtest.update.snapshots` to update",
      expected.toString(), actualContent.toString(), snapshotFile.toString())
  }
}

private fun isUpdateSnapshotIfMismatch(): Boolean {
  if (UsefulTestCase.IS_UNDER_TEAMCITY) {
    return false
  }

  val value = System.getProperty("test.update.snapshots")
  return value != null && (value.isEmpty() || value.toBoolean())
}
