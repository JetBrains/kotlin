// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events.impl;

import com.intellij.build.events.BuildIssueEvent;
import com.intellij.build.events.MessageEventResult;
import com.intellij.build.issue.BuildIssue;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class BuildIssueEventImpl extends AbstractBuildEvent implements BuildIssueEvent {
  private final BuildIssue myIssue;
  private final Kind myKind;

  public BuildIssueEventImpl(@NotNull Object parentId,
                             @NotNull BuildIssue buildIssue,
                             @NotNull Kind kind) {
    super(new Object(), parentId, System.currentTimeMillis(), buildIssue.getTitle());
    myIssue = buildIssue;
    myKind = kind;
  }

  @NotNull
  @Override
  public final String getDescription() {
    return myIssue.getDescription();
  }

  @NotNull
  @Override
  public BuildIssue getIssue() {
    return myIssue;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @NotNull
  @Override
  public String getGroup() {
    return "Build issues";
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@NotNull Project project) {
    return myIssue.getNavigatable(project);
  }

  @Override
  public MessageEventResult getResult() {
    return new MessageEventResult() {
      @Override
      public Kind getKind() {
        return myKind;
      }

      @NotNull
      @Override
      public String getDetails() {
        return myIssue.getDescription();
      }
    };
  }
}
