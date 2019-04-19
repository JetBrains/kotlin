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
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class DefaultFileCollectionDependency extends AbstractExternalDependency implements FileCollectionDependency {

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
