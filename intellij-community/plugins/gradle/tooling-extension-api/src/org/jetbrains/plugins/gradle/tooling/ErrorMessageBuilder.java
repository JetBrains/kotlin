// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.internal.impldep.com.google.gson.GsonBuilder;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Intended to be used only for reporting of custom {@link ModelBuilderService}s unhandled failures.
 * <p>
 * Use {@link ModelBuilderContext#report} for errors, warnings detected by your {@link ModelBuilderService}.
 *
 * @author Vladislav.Soroka
 * @see MessageBuilder
 * @see ModelBuilderContext#report
 */
public final class ErrorMessageBuilder {
  @NotNull private final Project myProject;
  @Nullable private final Exception myException;
  @NotNull private final String myGroup;
  @Nullable private String myDescription;

  private ErrorMessageBuilder(@NotNull Project project, @Nullable Exception exception, @NotNull String group) {
    myProject = project;
    myException = exception;
    myGroup = group;
  }

  public static ErrorMessageBuilder create(@NotNull Project project, @NotNull String group) {
    return new ErrorMessageBuilder(project, null, group);
  }

  public static ErrorMessageBuilder create(@NotNull Project project, @Nullable Exception exception, @NotNull String group) {
    return new ErrorMessageBuilder(project, exception, group);
  }

  public ErrorMessageBuilder withDescription(@NotNull String description) {
    myDescription = description;
    return this;
  }

  /**
   * @deprecated use {@link ModelBuilderContext#report} instead
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
  @Deprecated
  public String build() {
    Message message = buildMessage();
    return new GsonBuilder().create().toJson(message);
  }

  @ApiStatus.Internal
  public Message buildMessage() {
    String message = myDescription != null ? myDescription : "";
    String projectDisplayName = getDisplayName(myProject);
    String title = myException != null ? getRootCauseMessage(myException) : myDescription != null ? myDescription : myGroup;
    title = projectDisplayName + ": " + title;
    return MessageBuilder.create(title, message)
      .warning() // custom model builders failures often not so critical to the import results and reported as warnings to avoid useless distraction
      .withException(myException)
      .withGroup(myGroup)
      .withLocation(myProject.getBuildFile().getPath(), 0, 0)
      .build();
  }

  @NotNull
  private static String getDisplayName(@NotNull Project project) {
    String projectDisplayName;
    if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("3.3")) < 0) {
      StringBuilder builder = new StringBuilder();
      if (project.getParent() == null && project.getGradle().getParent() == null) {
        builder.append("root project '");
        builder.append(project.getName());
        builder.append('\'');
      }
      else {
        builder.append("project '");
        builder.append(project.getPath());
        builder.append("'");
      }
      projectDisplayName = builder.toString();
    }
    else {
      projectDisplayName = project.getDisplayName();
    }
    return projectDisplayName;
  }

  @NotNull
  private static String getRootCauseMessage(@NotNull Throwable e) {
    while (true) {
      if (e.getCause() == null) return e.getMessage();
      e = e.getCause();
    }
  }
}
