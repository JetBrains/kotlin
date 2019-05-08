// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.issue.BuildIssueData
import org.gradle.tooling.model.build.BuildEnvironment
import org.jetbrains.annotations.ApiStatus

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleIssueData(val buildEnvironment: BuildEnvironment?,
                      val projectPath: String,
                      val buildFilePath: String?,
                      val error: Throwable,
                      val rootCause: Throwable,
                      val location: String?) : BuildIssueData
