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
package org.jetbrains.plugins.gradle.model;

import org.gradle.internal.impldep.com.google.common.base.Objects;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalLibraryDependency extends AbstractExternalDependency implements ExternalLibraryDependency {

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
    return Objects.equal(file, that.file);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), file);
  }

  @Override
  public String toString() {
    return "library '" + file + '\'';
  }
}
