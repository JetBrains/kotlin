// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import org.jetbrains.plugins.gradle.execution.GradleConsoleFilter
import org.jetbrains.plugins.gradle.issue.GradleIssueChecker
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
      filter.applyFilter(location, location.length) ?: return false
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
        nextLine == "* Try:" -> {
          reader.pushBack()
          break@loop
        }
      }
    }

    var trySuggestions: StringBuilder? = null
    var exception: StringBuilder? = null
    while (true) {
      val nextLine = reader.readLine() ?: break
      if (nextLine == "BUILD FAILED" || nextLine == "* Get more help at https://help.gradle.org" || nextLine.startsWith("CONFIGURE FAILED")) break
      if (nextLine == "* Exception is:") {
        exception = StringBuilder()
      }
      else if (nextLine == "* Try:") {
        trySuggestions = StringBuilder()
      }
      else {
        if (exception != null) {
          exception.appendln(nextLine)
        }
        else if (trySuggestions != null && nextLine.isNotBlank()) {
          trySuggestions.appendln(nextLine)
        }
      }
    }

    // compilation errors should be added by the respective compiler output parser
    if (reason.startsWith("Compilation failed") ||
        reason == "Compilation error. See log for more details" ||
        reason == "Script compilation error:" ||
        reason.contains("compiler failed")) return false

    val filePosition: FilePosition?
    if (filter != null) {
      filePosition = FilePosition(File(filter.filteredFileName), filter.filteredLineNumber - 1, 0)
    }
    else {
      filePosition = null
    }

    val errorText = description.toString()
    for (issueChecker in GradleIssueChecker.getKnownIssuesCheckList()) {
      if (issueChecker.consumeBuildOutputFailureMessage(errorText, reason, exception.toString(), filePosition, parentId, messageConsumer)) {
        return true
      }
    }

    val detailedMessage = StringBuilder(errorText)
    if (!trySuggestions.isNullOrBlank()) {
      detailedMessage.append("\n* Try:\n$trySuggestions")
    }
    if (!exception.isNullOrBlank()) {
      detailedMessage.append("\n* Exception is:\n$exception")
    }
    if (filePosition != null) {
      messageConsumer.accept(object : FileMessageEventImpl(
        parentId, MessageEvent.Kind.ERROR, null, reason, detailedMessage.toString(), filePosition), DuplicateMessageAware {}
      )
    }
    else {
      val unresolvedMessageEvent = checkUnresolvedDependencyError(reason, errorText, parentId)
      if (unresolvedMessageEvent != null) {
        messageConsumer.accept(unresolvedMessageEvent)
      }
      else {
        messageConsumer.accept(object : MessageEventImpl(parentId, MessageEvent.Kind.ERROR, null, reason,
                                                         detailedMessage.toString()), DuplicateMessageAware {})
      }
    }
    return true
  }

  private fun checkUnresolvedDependencyError(reason: String, description: String, parentId: Any): BuildEvent? {
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
    val unresolvedDependencyIssue = UnresolvedDependencyBuildIssue(dependencyName, description, indexOfSuffix > 0)
    return BuildIssueEventImpl(parentId, unresolvedDependencyIssue, MessageEvent.Kind.ERROR)
  }
}

