// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class WebResource implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final WarDirectory warDirectory;
  @NotNull
  private final String warRelativePath;
  @NotNull
  private final File file;

  @PropertyMapping({"warDirectory", "warRelativePath", "file"})
  public WebResource(@NotNull WarDirectory warDirectory, @NotNull String warRelativePath, @NotNull File file) {
    this.warDirectory = warDirectory;
    this.warRelativePath = getAdjustedPath(warRelativePath);
    this.file = file;
  }

  @NotNull
  public WarDirectory getWarDirectory() {
    return warDirectory;
  }

  @NotNull
  public String getWarRelativePath() {
    return warRelativePath;
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
    if (!(o instanceof WebResource)) return false;

    WebResource resource = (WebResource)o;
    if (!file.getPath().equals(resource.file.getPath())) return false;
    if (warDirectory != resource.warDirectory) return false;
    if (!warRelativePath.equals(resource.warRelativePath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = warDirectory.hashCode();
    result = 31 * result + warRelativePath.hashCode();
    result = 31 * result + file.getPath().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "WebResource{" +
           "myWarDirectory=" + warDirectory +
           ", warRelativePath='" + warRelativePath + '\'' +
           ", file=" + file +
           '}';
  }
}
