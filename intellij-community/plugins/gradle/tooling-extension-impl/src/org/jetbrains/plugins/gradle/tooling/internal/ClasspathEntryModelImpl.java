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
package org.jetbrains.plugins.gradle.tooling.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ClasspathEntryModel;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class ClasspathEntryModelImpl implements ClasspathEntryModel, Serializable {
  @NotNull
  private final Set<String> classes;
  @NotNull
  private final Set<String> sources;
  @NotNull
  private final Set<String> javadoc;

  public ClasspathEntryModelImpl(@NotNull Set<String> classes, @NotNull Set<String> sources, @NotNull Set<String> javadoc) {
    this.classes = classes;
    this.sources = sources;
    this.javadoc = javadoc;
  }

  @NotNull
  @Override
  public Set<String> getClasses() {
    return classes;
  }

  @NotNull
  @Override
  public Set<String> getSources() {
    return sources;
  }

  @NotNull
  @Override
  public Set<String> getJavadoc() {
    return javadoc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClasspathEntryModelImpl model = (ClasspathEntryModelImpl)o;

    if (!classes.equals(model.classes)) return false;
    if (!sources.equals(model.sources)) return false;
    if (!javadoc.equals(model.javadoc)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = classes.hashCode();
    result = 31 * result + sources.hashCode();
    result = 31 * result + javadoc.hashCode();
    return result;
  }
}
