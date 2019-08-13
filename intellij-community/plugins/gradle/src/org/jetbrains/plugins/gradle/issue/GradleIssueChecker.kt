// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.issue.BuildIssueChecker
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
interface GradleIssueChecker : BuildIssueChecker<GradleIssueData> {
  companion object {
    internal val EP = ExtensionPointName.create<GradleIssueChecker>("org.jetbrains.plugins.gradle.issueChecker")
    @JvmStatic
    fun getKnownIssuesCheckList(): List<GradleIssueChecker> {
      return EP.extensionList
    }
  }
}
