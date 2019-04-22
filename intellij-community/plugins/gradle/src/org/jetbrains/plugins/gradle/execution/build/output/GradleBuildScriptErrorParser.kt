// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build.output

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import org.jetbrains.plugins.gradle.execution.GradleConsoleFilter
import java.io.File
import java.util.function.Consumer

/**
 * @author Vladislav.Soroka
 */
class GradleBuildScriptErrorParser : BuildOutputParser {

  override fun parse(line: String, reader: BuildOutputInstantReader, messageConsumer: Consumer<in BuildEvent>): Boolean {
    if (!line.startsWith("FAILURE: Build failed with an exception.")) return false
    if (!reader.readLine().isNullOrBlank()) return false

    if (reader.readLine() != "* Where:") return false

    val location = reader.readLine() ?: return false
    val filter = GradleConsoleFilter(null)
    filter.applyFilter(location, 0) ?: return false

    if (!reader.readLine().isNullOrBlank()) return false

    if (reader.readLine() != "* What went wrong:") return false

    val description = StringBuilder().appendln(location).appendln()
    val reason = reader.readLine() ?: return false
    val parentId: Any
    if (reason.startsWith("Execution failed for task '")) {
      parentId = reason.substringAfter("Execution failed for task '").substringBefore("'.")
    }
    else {
      parentId = reader.parentEventId
    }
    description.appendln(reason)
    loop@ while (true) {
      val nextLine = reader.readLine() ?: return false
      if (nextLine.isBlank()) break
      description.appendln(nextLine)
      when {
        nextLine.isEmpty() -> break@loop
        nextLine == "* Try:" -> break@loop
      }
    }
    val filePosition = FilePosition(File(filter.filteredFileName), filter.filteredLineNumber - 1, 0)
    messageConsumer.accept(FileMessageEventImpl(parentId, MessageEvent.Kind.ERROR, null, reason, description.toString(), filePosition))
    return true
  }
}

