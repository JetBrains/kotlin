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

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel;
import org.jetbrains.plugins.gradle.model.ClasspathEntryModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class BuildScriptClasspathModelImpl implements BuildScriptClasspathModel {

  private final List<ClasspathEntryModel> myClasspathEntries;
  @Nullable
  private File gradleHomeDir;
  private String myGradleVersion;

  public BuildScriptClasspathModelImpl() {
    myClasspathEntries = new ArrayList<ClasspathEntryModel>(0);
  }

  @Override
  public DomainObjectSet<? extends ClasspathEntryModel> getClasspath() {
    return ImmutableDomainObjectSet.of(myClasspathEntries);
  }

  public void setGradleHomeDir(@Nullable File file) {
    gradleHomeDir = file;
  }

  @Nullable
  @Override
  public File getGradleHomeDir() {
    return gradleHomeDir;
  }

  public void add(@NotNull ClasspathEntryModel classpathEntryModel) {
    myClasspathEntries.add(classpathEntryModel);
  }

  public void setGradleVersion(@NotNull String gradleVersion) {
    myGradleVersion = gradleVersion;
  }

  @NotNull
  @Override
  public String getGradleVersion() {
    return myGradleVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BuildScriptClasspathModelImpl other = (BuildScriptClasspathModelImpl)o;

    if (!myClasspathEntries.equals(other.myClasspathEntries)) return false;
    if (gradleHomeDir == null && other.gradleHomeDir != null ||
        gradleHomeDir != null && (other.gradleHomeDir == null || !gradleHomeDir.getPath().equals(other.gradleHomeDir.getPath()))) {
      return false;
    }
    if (myGradleVersion != null ? !myGradleVersion.equals(other.myGradleVersion) : other.myGradleVersion != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myClasspathEntries.hashCode();
    result = 31 * result + (gradleHomeDir != null ? gradleHomeDir.hashCode() : 0);
    result = 31 * result + (myGradleVersion != null ? myGradleVersion.hashCode() : 0);
    return result;
  }
}
