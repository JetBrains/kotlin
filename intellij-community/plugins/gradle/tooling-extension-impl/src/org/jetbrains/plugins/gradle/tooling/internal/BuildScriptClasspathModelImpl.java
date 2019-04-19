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
  @NotNull
  private String myGradleVersion;

  public BuildScriptClasspathModelImpl() {
    myClasspathEntries = new ArrayList<ClasspathEntryModel>();
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
}
