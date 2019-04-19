/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class EarResource implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final String myEarDirectory;
  @NotNull
  private final String relativePath;
  @NotNull
  private final File file;

  public EarResource(@NotNull String earDirectory, @NotNull String relativePath, @NotNull File file) {
    myEarDirectory = earDirectory;
    this.relativePath = getAdjustedPath(relativePath);
    this.file = file;
  }

  @NotNull
  public String getEarDirectory() {
    return myEarDirectory;
  }

  @NotNull
  public String getRelativePath() {
    return relativePath;
  }

  @NotNull
  public File getFile() {
    return file;
  }

  private static String getAdjustedPath(final @NotNull String path) {
    return path.isEmpty() || path.charAt(0) != '/' ? '/' + path : path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EarResource)) return false;

    EarResource resource = (EarResource)o;
    if (!file.getPath().equals(resource.file.getPath())) return false;
    if (myEarDirectory != resource.myEarDirectory) return false;
    if (!relativePath.equals(resource.relativePath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myEarDirectory.hashCode();
    result = 31 * result + relativePath.hashCode();
    result = 31 * result + file.getPath().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Resource{" +
           "earDirectory=" + myEarDirectory +
           ", relativePath='" + relativePath + '\'' +
           ", file=" + file +
           '}';
  }
}
