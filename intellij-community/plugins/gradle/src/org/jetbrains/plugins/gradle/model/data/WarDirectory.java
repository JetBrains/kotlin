/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class WarDirectory implements Serializable {
  /**
   * Public files typically include the following:
   * HTML files.
   * JSP files.
   * Image files and other multimedia files -- it is a common convention to store image files in an images subdirectory.
   */
  public static final WarDirectory WAR_ROOT = new WarDirectory("/");
  /**
   * directory can contain the following file:
   * META-INF/MANIFEST.MF -- an optional file that can be used to specify additional meta-information for the WAR.
   */
  public static final WarDirectory META_INF = new WarDirectory("/META-INF");
  /**
   * Directory contains a Web archive's private files and directories.
   * That is, when the Web archive is deployed, the files and directories under the WEB-INF/ directory cannot be accessed directly by Web clients.
   */
  public static final WarDirectory WEB_INF = new WarDirectory("/WEB-INF");
  /**
   * Subdirectory can store JAR files used by the Web module.
   * The JAR files in this directory are automatically accessible to the Web module without needing to be added to the class path.
   */
  public static final WarDirectory WEB_INF_LIB = new WarDirectory("/WEB-INF/lib");
  /**
   * Spring-Boot convention directory of provided library
   * Subdirectory can store JAR files used by the Web module,
   * that are required when running embedded but are not required when deploying to a traditional web container.
   */
  public static final WarDirectory WEB_INF_LIB_PROVIDED = new WarDirectory("/WEB-INF/lib-provided");
  /**
   * Subdirectory contains the compiled Java code for the Web module.
   */
  public static final WarDirectory WEB_INF_CLASSES = new WarDirectory("/WEB-INF/classes");

  private static final WarDirectory[] WAR_DIRECTORIES =
    new WarDirectory[]{WAR_ROOT, META_INF, WEB_INF, WEB_INF_LIB, WEB_INF_LIB_PROVIDED, WEB_INF_CLASSES};

  @NotNull
  private final String myRelativePath;

  WarDirectory(@NotNull final String relativePath) {
    myRelativePath = getAdjustedPath(relativePath);
  }

  @NotNull
  public String getRelativePath() {
    return myRelativePath;
  }

  public boolean isCustomDirectory() {
    for (WarDirectory warDirectory : WAR_DIRECTORIES) {
      if (myRelativePath.equals(warDirectory.getRelativePath())) return false;
    }
    return true;
  }

  @NotNull
  public static WarDirectory fromPath(final @NotNull String path) {
    if (StringUtil.isEmpty(path)) return WAR_ROOT;

    final String adjustedPath = getAdjustedPath(path);
    for (WarDirectory warDirectory : WAR_DIRECTORIES) {
      if (warDirectory.myRelativePath.equals(adjustedPath)) return warDirectory;
    }
    return new WarDirectory(adjustedPath);
  }

  private static String getAdjustedPath(final @NotNull String path) {
    return path.isEmpty() || path.charAt(0) != '/' ? '/' + path : path;
  }

  @Override
  public String toString() {
    return myRelativePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WarDirectory directory = (WarDirectory)o;
    if (!myRelativePath.equals(directory.myRelativePath)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return myRelativePath.hashCode();
  }
}
