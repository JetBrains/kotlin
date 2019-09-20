// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.internal.impldep.com.google.common.base.Objects;

import java.io.File;

public final class DefaultExternalLibraryDependency extends AbstractExternalDependency implements ExternalLibraryDependency {
  private static final long serialVersionUID = 1L;

  private File file;
  private File source;
  private File javadoc;

  public DefaultExternalLibraryDependency() {
  }

  public DefaultExternalLibraryDependency(ExternalLibraryDependency dependency) {
    super(dependency);
    file = dependency.getFile();
    source = dependency.getSource();
    javadoc = dependency.getJavadoc();
  }

  @Override
  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  @Override
  public File getSource() {
    return source;
  }

  public void setSource(File source) {
    this.source = source;
  }

  @Override
  public File getJavadoc() {
    return javadoc;
  }

  public void setJavadoc(File javadoc) {
    this.javadoc = javadoc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultExternalLibraryDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultExternalLibraryDependency that = (DefaultExternalLibraryDependency)o;
    return Objects.equal(file.getPath(), that.file.getPath());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), file.getPath());
  }

  @Override
  public String toString() {
    return "library '" + file + '\'';
  }
}
