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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalMultiLibraryDependency extends AbstractExternalDependency implements ExternalMultiLibraryDependency {

  private static final long serialVersionUID = 1L;
  private Collection<File> files = new LinkedHashSet<File>();
  private Collection<File> sources = new LinkedHashSet<File>();
  private Collection<File> javadocs = new LinkedHashSet<File>();

  public DefaultExternalMultiLibraryDependency() {
  }

  public DefaultExternalMultiLibraryDependency(ExternalMultiLibraryDependency dependency) {
    super(dependency);
    files = dependency.getFiles();
    sources = dependency.getSources();
    javadocs = dependency.getJavadoc();
  }

  @NotNull
  @Override
  public Collection<File> getFiles() {
    return files;
  }

  @NotNull
  @Override
  public Collection<File> getSources() {
    return sources;
  }

  @NotNull
  @Override
  public Collection<File> getJavadoc() {
    return javadocs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultExternalMultiLibraryDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultExternalMultiLibraryDependency that = (DefaultExternalMultiLibraryDependency)o;
    return Objects.equal(files, that.files);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), files);
  }

  @Override
  public String toString() {
    return "library '" + files + '\'';
  }
}
