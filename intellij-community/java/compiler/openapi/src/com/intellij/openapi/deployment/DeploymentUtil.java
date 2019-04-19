/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.deployment;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.descriptors.ConfigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class DeploymentUtil {
  public static DeploymentUtil getInstance() {
    return ServiceManager.getService(DeploymentUtil.class);
  }

  public static String trimForwardSlashes(@NotNull String path) {
    while (path.length() != 0 && (path.charAt(0) == '/' || path.charAt(0) == File.separatorChar)) {
      path = path.substring(1);
    }
    return path;
  }

  public static String concatPaths(String... paths) {
    final StringBuilder builder = new StringBuilder();
    for (String path : paths) {
      if (path.length() == 0) continue;

      final int len = builder.length();
      if (len > 0 && builder.charAt(len - 1) != '/' && builder.charAt(len - 1) != File.separatorChar) {
        builder.append('/');
      }
      builder.append(len != 0 ? trimForwardSlashes(path) : path);
    }
    return builder.toString();
  }

  public static String appendToPath(@NotNull String basePath, @NotNull String relativePath) {
    final boolean endsWithSlash = StringUtil.endsWithChar(basePath, '/') || StringUtil.endsWithChar(basePath, '\\');
    final boolean startsWithSlash = StringUtil.startsWithChar(relativePath, '/') || StringUtil.startsWithChar(relativePath, '\\');
    String tail;
    if (endsWithSlash && startsWithSlash) {
      tail = trimForwardSlashes(relativePath);
    }
    else if (!endsWithSlash && !startsWithSlash && basePath.length() > 0 && relativePath.length() > 0) {
      tail = "/" + relativePath;
    }
    else {
      tail = relativePath;
    }
    return basePath + tail;
  }

  @Nullable
  public abstract String getConfigFileErrorMessage(ConfigFile configFile);

  /**
   * @deprecated use {@link com.intellij.openapi.util.io.FileUtil#getRelativePath}
   */
  @Deprecated
  @Nullable
  public static String getRelativePath(@NotNull String basePath, @NotNull final String filePath) {
    if (basePath.equals(filePath)) return "";
    if (!basePath.endsWith(File.separator)) basePath += File.separatorChar;

    int len = 0;
    int lastSeparatorIndex = 0; // need this for cases like this: base="/temp/abcde/baseDir" and file="/temp/ab"
    while (len < filePath.length() && len < basePath.length() && filePath.charAt(len) == basePath.charAt(len)) {
      if (basePath.charAt(len) == File.separatorChar) {
        lastSeparatorIndex = len;
      }
      len++;
    }

    if (len == 0) {
      return null;
    }
    final StringBuilder relativePath = new StringBuilder();
    for (int i=len; i < basePath.length(); i++) {
      if (basePath.charAt(i) == File.separatorChar) {
        relativePath.append("..");
        relativePath.append(File.separatorChar);
      }
    }
    relativePath.append(filePath.substring(lastSeparatorIndex + 1));

    return relativePath.toString();
  }
}
