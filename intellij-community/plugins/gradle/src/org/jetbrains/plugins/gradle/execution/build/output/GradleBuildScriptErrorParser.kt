// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build.output

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.DuplicateMessageAware
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.BuildIssueEventImpl
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.util.text.nullize
import org.jetbrains.plugins.gradle.execution.GradleConsoleFilter
import org.jetbrains.plugins.gradle.issue.UnresolvedDependencyBuildIssue
import java.io.File
import java.util.function.Consumer

/**
 * @author Vladislav.Soroka
 */
class GradleBuildScriptErrorParser : BuildOutputParser {

  override fun parse(line: String, reader: BuildOutputInstantReader, messageConsumer: Consumer<in BuildEvent>): Boolean {
    if (!line.startsWith("FAILURE: Build failed with an exception.")) return false
    if (!reader.readLine().isNullOrBlank()) return false

    val location: String?
    val filter: GradleConsoleFilter?
    var whereOrWhatLine = reader.readLine()
    if (whereOrWhatLine == "* Where:") {
      location = reader.readLine() ?: return false
      filter = GradleConsoleFilter(null)
      filter.applyFilter(location, 0) ?: return false
      if (!reader.readLine().isNullOrBlank()) return false
      whereOrWhatLine = reader.readLine()
    }
    else {
      location = null
      filter = null
    }

    if (whereOrWhatLine != "* What went wrong:") return false

    val description = StringBuilder()
    if (location != null) {
      description.appendln(location).appendln()
    }
    var reason = reader.readLine() ?: return false
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
      val trimStart = nextLine.trimStart()
      if (trimStart.startsWith("> ")) {
        reason = trimStart.substringAfter("> ").trimEnd('.')
      }
      when {
        nextLine.isEmpty() -> break@loop
        nextLine == "* Try:" -> break@loop
      }
    }

    var exception: StringBuilder? = null
    while (true) {
      val nextLine = reader.readLine() ?: break
      if (nextLine == "* Exception is:")  {
        exception = StringBuilder(nextLine).appendln()
      } else {
        exception?.appendln(nextLine)
      }
      if (nextLine == "BUILD FAILED") break
    }
    exception?.also { description.appendln().append(it) }

    // compilation errors should be added by the respective compiler output parser
    if (reason.startsWith("Compilation failed") ||
        reason == "Compilation error. See log for more details" ||
        reason == "Script compilation error:" ||
        reason.contains("compiler failed")) return false

    // JDK compatibility issues should be handled by org.jetbrains.plugins.gradle.issue.IncompatibleGradleJdkIssueChecker
    if (reason.startsWith("Could not create service of type ") && reason.contains(" using BuildScopeServices.")) return false

    // Build cancellation errors should be handled by org.jetbrains.plugins.gradle.issue.GradleBuildCancelledIssueChecker
    if (reason.contains("Build cancelled.")) return false

    if (location != null && filter != null) {
      val errorText = description.toString()
      val reasonAndFilePosition = getReasonAndFilePosition(reason, errorText, filter)
      reason = reasonAndFilePosition.first
      val filePosition = reasonAndFilePosition.second
      messageConsumer.accept(object : FileMessageEventImpl(
        parentId, MessageEvent.Kind.ERROR, null, reason, errorText, filePosition), DuplicateMessageAware {}
      )
    }
    else {
      val unresolvedMessageEvent = checkUnresolvedDependencyError(reason, description, parentId)
      if (unresolvedMessageEvent != null) {
        messageConsumer.accept(unresolvedMessageEvent)
      }
      else {
        messageConsumer.accept(object : MessageEventImpl(parentId, MessageEvent.Kind.ERROR, null, reason,
                                                         description.toString()), DuplicateMessageAware {})
      }
    }
    return true
  }

  private fun getReasonAndFilePosition(reason: String, errorText: String, filter: GradleConsoleFilter): Pair<String, FilePosition> {
    if (reason == "startup failed:") {
      val startupError = getStartupErrorReasonAndFilePosition(errorText, filter)
      if (startupError != null) return startupError
    }

    return Pair(reason, FilePosition(File(filter.filteredFileName), filter.filteredLineNumber - 1, 0))
  }

  private fun getStartupErrorReasonAndFilePosition(errorText: String, filter: GradleConsoleFilter): Pair<String, FilePosition>? {
    val locationLine = errorText.substringAfter("> startup failed:", "").nullize()?.trimStart()?.substringBefore("\n") ?: return null
    val failedStartupReason = locationLine.substringAfter("'${filter.filteredFileName}': ${filter.filteredLineNumber}: ", "")
                                .nullize()?.substringBeforeLast(" @ ") ?: return null
    val locationPart = locationLine.substringAfterLast(" @ ")
    val matchResult = GradleConsoleFilter.LINE_AND_COLUMN_PATTERN.toRegex().matchEntire(locationPart)
    val values = matchResult?.groupValues?.drop(1)?.map { it.toInt() } ?: listOf(filter.filteredLineNumber, 0)
    val line = values[0] - 1
    val column = values[1]
    return Pair(failedStartupReason, FilePosition(File(filter.filteredFileName), line, column))
  }

  private fun checkUnresolvedDependencyError(reason: String, description: StringBuilder, parentId: Any): BuildEvent? {
    val noCachedVersionPrefix = "No cached version of "
    val couldNotFindPrefix = "Could not find "
    val cannotResolvePrefix = "Cannot resolve external dependency "
    val cannotDownloadPrefix = "Could not download "
    val prefix = when {
                   reason.startsWith(noCachedVersionPrefix) -> noCachedVersionPrefix
                   reason.startsWith(couldNotFindPrefix) -> couldNotFindPrefix
                   reason.startsWith(cannotResolvePrefix) -> cannotResolvePrefix
                   reason.startsWith(cannotDownloadPrefix) -> cannotDownloadPrefix
                   else -> null
                 } ?: return null
    val indexOfSuffix = reason.indexOf(" available for offline mode")
    val dependencyName = if (indexOfSuffix > 0) reason.substring(prefix.length, indexOfSuffix) else reason.substring(prefix.length)
    val unresolvedDependencyIssue = UnresolvedDependencyBuildIssue(dependencyName, description.toString(), indexOfSuffix > 0)
    return BuildIssueEventImpl(parentId, unresolvedDependencyIssue, MessageEvent.Kind.ERROR)
  }
}

