/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.LocationAwareExternalSystemException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.util.text.StringUtil.splitByLines;

/**
 * @author Vladislav.Soroka
 */
public class GradleExecutionErrorHandler {
  public static final Pattern UNSUPPORTED_GRADLE_VERSION_ERROR_PATTERN;
  public static final Pattern DOWNLOAD_GRADLE_DISTIBUTION_ERROR_PATTERN;
  public static final Pattern MISSING_METHOD_PATTERN;
  public static final Pattern ERROR_LOCATION_IN_FILE_PATTERN;
  public static final Pattern ERROR_IN_FILE_PATTERN;

  static {
    UNSUPPORTED_GRADLE_VERSION_ERROR_PATTERN = Pattern.compile("Gradle version .* is required.*");
    DOWNLOAD_GRADLE_DISTIBUTION_ERROR_PATTERN = Pattern.compile("The specified Gradle distribution .* does not exist.");
    MISSING_METHOD_PATTERN = Pattern.compile("org.gradle.api.internal.MissingMethodException: Could not find method (.*?) .*");
    ERROR_LOCATION_IN_FILE_PATTERN = Pattern.compile("(?:Build|Settings) file '(.*)' line: ([\\d]+)");
    ERROR_IN_FILE_PATTERN = Pattern.compile("(?:Build|Settings) file '(.*)'");
  }

  private final Throwable myOriginError;
  private final ExternalSystemException myFriendlyError;
  private Pair<Throwable, String> myRootCauseAndLocation;

  public GradleExecutionErrorHandler(@NotNull Throwable error,
                                     @NotNull String projectPath,
                                     @Nullable String buildFilePath) {
    myOriginError = error;
    myFriendlyError = getUserFriendlyError(error, projectPath, buildFilePath);
  }

  public ExternalSystemException getUserFriendlyError() {
    return myFriendlyError;
  }

  public Throwable getRootCause() {
    if (myRootCauseAndLocation == null) {
      myRootCauseAndLocation = getRootCauseAndLocation(myOriginError);
    }
    return myRootCauseAndLocation.first;
  }

  public String getLocation() {
    if (myRootCauseAndLocation == null) {
      myRootCauseAndLocation = getRootCauseAndLocation(myOriginError);
    }
    return myRootCauseAndLocation.second;
  }

  @Nullable
  private ExternalSystemException getUserFriendlyError(@NotNull Throwable error,
                                                       @NotNull String projectPath,
                                                       @Nullable String buildFilePath) {
    if (error instanceof ExternalSystemException) {
      // This is already a user-friendly error.
      return (ExternalSystemException)error;
    }

    if (myRootCauseAndLocation == null) {
      myRootCauseAndLocation = getRootCauseAndLocation(error);
    }
    if (myRootCauseAndLocation.first instanceof FileNotFoundException) {
      Throwable errorCause = error.getCause();
      if (errorCause instanceof IllegalArgumentException &&
          DOWNLOAD_GRADLE_DISTIBUTION_ERROR_PATTERN.matcher(errorCause.getMessage()).matches()) {
        return createUserFriendlyError(errorCause.getMessage(), null);
      }
    }
    return null;
  }

  @NotNull
  public static Pair<Throwable, String> getRootCauseAndLocation(@NotNull Throwable error) {
    Throwable rootCause = error;
    String location = null;
    while (true) {
      if (location == null) {
        location = getLocationFrom(rootCause);
      }
      Throwable cause = rootCause.getCause();
      if (cause == null || cause.getMessage() == null && !(cause instanceof StackOverflowError)) {
        break;
      }
      rootCause = cause;
    }
    return Pair.create(rootCause, location);
  }


  /**
   * Retrieves the error location in build.gradle files or in settings.gradle file.
   */
  @Nullable
  public static String getLocationFrom(@NotNull Throwable error) {
    String errorToString = error.toString();
    if (errorToString.contains("LocationAwareException")) {
      // LocationAwareException is never passed, but converted into a PlaceholderException
      // that has the toString value of the original LocationAwareException.
      String location = error.getMessage();
      if (location != null && (location.startsWith("Build file '") || location.startsWith("Settings file '"))) {
        // Only the first line contains the location of the error. Discard the rest.
        String[] lines = splitByLines(location);
        return lines.length > 0 ? lines[0] : null;
      }
    }
    return null;
  }

  @NotNull
  public static ExternalSystemException createUserFriendlyError(@NotNull String msg,
                                                                @Nullable String location,
                                                                @NotNull String... quickFixes) {
    String newMsg = msg;
    if (!newMsg.isEmpty() && Character.isLowerCase(newMsg.charAt(0))) {
      // Message starts with lower case letter. Sentences should start with uppercase.
      newMsg = "Cause: " + newMsg;
    }

    if (!StringUtil.isEmpty(location)) {
      Pair<String, Integer> pair = getErrorLocation(location);
      if (pair != null) {
        return new LocationAwareExternalSystemException(newMsg, pair.first, pair.getSecond(), quickFixes);
      }
    }
    return new ExternalSystemException(newMsg, null, quickFixes);
  }

  @Nullable
  public static Pair<String, Integer> getErrorLocation(@NotNull String location) {
    Matcher matcher = ERROR_LOCATION_IN_FILE_PATTERN.matcher(location);
    if (matcher.matches()) {
      String filePath = matcher.group(1);
      int line = -1;
      try {
        line = Integer.parseInt(matcher.group(2));
      }
      catch (NumberFormatException e) {
        // ignored.
      }
      return Pair.create(filePath, line);
    }

    matcher = ERROR_IN_FILE_PATTERN.matcher(location);
    if (matcher.matches()) {
      String filePath = matcher.group(1);
      return Pair.create(filePath, -1);
    }
    return null;
  }
}
