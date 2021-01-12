// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;
import org.jetbrains.plugins.gradle.tooling.util.BooleanBiFunction;
import org.jetbrains.plugins.gradle.tooling.util.GradleContainerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public final class DefaultFileCollectionDependency extends AbstractExternalDependency implements FileCollectionDependency {
  private static final long serialVersionUID = 1L;

  private final Collection<File> files;
  private boolean excludedFromIndexing;

  public DefaultFileCollectionDependency() {
    this(new ArrayList<File>());
  }

  public DefaultFileCollectionDependency(Collection<File> files) {
    super(new DefaultExternalDependencyId(null, files.toString(), null), null, null);
    this.files = new ArrayList<File>(files);
  }

  public DefaultFileCollectionDependency(FileCollectionDependency dependency) {
    super(dependency);
    files = new ArrayList<File>(dependency.getFiles());
  }

  @NotNull
  @Override
  public Collection<File> getFiles() {
    return files;
  }

  @ApiStatus.Experimental
  public boolean isExcludedFromIndexing() {
    return excludedFromIndexing;
  }

  @ApiStatus.Experimental
  public void setExcludedFromIndexing(boolean excludedFromIndexing) {
    this.excludedFromIndexing = excludedFromIndexing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultFileCollectionDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultFileCollectionDependency that = (DefaultFileCollectionDependency)o;
    return GradleContainerUtil.match(files.iterator(), that.files.iterator(), new BooleanBiFunction<File, File>() {
      @Override
      public Boolean fun(File o1, File o2) {
        return Objects.equal(o1.getPath(), o2.getPath());
      }
    });
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), calcFilesPathsHashCode(files));
  }

  @Override
  public String toString() {
    return "file collection dependency{" +
           "files=" + files +
           '}';
  }
}
