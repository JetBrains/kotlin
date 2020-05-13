// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.issue.BuildIssueChecker
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import java.util.function.Consumer

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
interface GradleIssueChecker : BuildIssueChecker<GradleIssueData> {
  /**
   * Allows to customize Gradle build output failure message handling.
   * The format of this piece of the build output:
   *
   *    * Where:
   *    <...>
   *    * What went wrong:
   *    <...>
   *    * Try:
   *    <...>
   *    * Exception is:
   *    <...>
   *
   * @param message full textual presentation of the failure
   * @param failureCause extracted error message of the cause exception
   * @param stacktrace textual presentation of the exception stacktrace if any when the build was started with '--stacktrace' option enabled
   * @param location location of the failure if any
   * @param parentEventId the parent message id for [BuildEvent] construction
   * @param messageConsumer consumes generated [BuildEvent] based on this piece of build output
   *
   * @return true if related error message was added by this [GradleIssueChecker] using [messageConsumer] or via [GradleIssueChecker.check] method,
   *         so no further messages should be added to avoid duplicates
   */
  fun consumeBuildOutputFailureMessage(
    message: String,
    failureCause: String,
    stacktrace: String?,
    location: FilePosition?,
    parentEventId: Any,
    messageConsumer: Consumer<in BuildEvent>
  ): Boolean = false

  companion object {
    internal val EP = ExtensionPointName.create<GradleIssueChecker>("org.jetbrains.plugins.gradle.issueChecker")
    @JvmStatic
    fun getKnownIssuesCheckList(): List<GradleIssueChecker> {
      return EP.extensionList
    }
  }
}
