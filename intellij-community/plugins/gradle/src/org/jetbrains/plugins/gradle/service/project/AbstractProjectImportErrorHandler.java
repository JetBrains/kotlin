/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionErrorHandler;

import java.util.regex.Matcher;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractProjectImportErrorHandler {
  public static final String OPEN_GRADLE_SETTINGS = "Please fix the project's Gradle settings.";
  public static final String SET_UP_HTTP_PROXY =
    "If you are behind an HTTP proxy, please configure the proxy settings either in IDE or Gradle.";
  public static final String UNEXPECTED_ERROR_FILE_BUG = "This is an unexpected error. Please file a bug containing the idea.log file.";
  public static final String FIX_GRADLE_VERSION =
    "Please point to a supported Gradle version in the project's Gradle settings or in the project's Gradle wrapper (if applicable.)";
  public static final String EMPTY_LINE = "\n\n";

  @Nullable
  public abstract ExternalSystemException getUserFriendlyError(@Nullable BuildEnvironment buildEnvironment,
                                                               @NotNull Throwable error,
                                                               @NotNull String projectPath,
                                                               @Nullable String buildFilePath);

  @NotNull
  public ExternalSystemException createUserFriendlyError(@NotNull String msg, @Nullable String location, String @NotNull ... quickFixes) {
    return GradleExecutionErrorHandler.createUserFriendlyError(msg, location, quickFixes);
  }

  @NotNull
  public String parseMissingMethod(@NotNull String rootCauseText) {
    Matcher matcher = GradleExecutionErrorHandler.MISSING_METHOD_PATTERN.matcher(rootCauseText);
    return matcher.find() ? matcher.group(1) : "";
  }
}
