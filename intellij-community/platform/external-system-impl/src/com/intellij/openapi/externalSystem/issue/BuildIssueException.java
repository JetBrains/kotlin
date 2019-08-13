// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.issue;

import com.intellij.build.issue.BuildIssue;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class BuildIssueException extends ExternalSystemException {
  private final BuildIssue myBuildIssue;

  public BuildIssueException(@NotNull BuildIssue issue) {
    super(issue.getDescription(), getQuickfixIds(issue));
    myBuildIssue = issue;
  }

  public BuildIssue getBuildIssue() {
    return myBuildIssue;
  }

  private static String[] getQuickfixIds(@NotNull BuildIssue issue) {
    return issue.getQuickFixes().stream().map(fix -> fix.getId()).toArray(String[]::new);
  }
}
