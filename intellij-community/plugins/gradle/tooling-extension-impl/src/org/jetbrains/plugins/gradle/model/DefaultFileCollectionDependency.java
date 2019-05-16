// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public final class DefaultFileCollectionDependency extends AbstractExternalDependency implements FileCollectionDependency {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final DefaultExternalDependencyId id;
  private final Collection<File> files;

  public DefaultFileCollectionDependency() {
    this(new ArrayList<File>());
  }

  public DefaultFileCollectionDependency(Collection<File> files) {
    this.files = new ArrayList<File>(files);
    id = new DefaultExternalDependencyId(null, files.toString(), null);
  }

  public DefaultFileCollectionDependency(FileCollectionDependency dependency) {
    super(dependency);
    files = new ArrayList<File>(dependency.getFiles());
    id = new DefaultExternalDependencyId(null, files.toString(), null);
  }

  @NotNull
  @Override
  public Collection<File> getFiles() {
    return files;
  }

  @NotNull
  @Override
  public DefaultExternalDependencyId getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultFileCollectionDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultFileCollectionDependency that = (DefaultFileCollectionDependency)o;
    return Objects.equal(files, that.files);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), files);
  }

  @Override
  public String toString() {
    return "file collection dependency{" +
           "files=" + files +
           '}';
  }
}
