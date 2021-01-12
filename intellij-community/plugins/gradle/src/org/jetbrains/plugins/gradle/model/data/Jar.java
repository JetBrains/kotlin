// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class Jar implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final String name;
  @Nullable
  private File archivePath;

  @Nullable
  private String manifestContent;

  @PropertyMapping({"name"})
  public Jar(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @Nullable
  public String getManifestContent() {
    return manifestContent;
  }

  public void setManifestContent(@Nullable String manifestContent) {
    this.manifestContent = manifestContent;
  }

  @Nullable
  public File getArchivePath() {
    return archivePath;
  }

  public void setArchivePath(@Nullable File archivePath) {
    this.archivePath = archivePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Jar that = (Jar)o;
    if (!name.equals(that.name)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Jar{'" + name + "'}";
  }
}
