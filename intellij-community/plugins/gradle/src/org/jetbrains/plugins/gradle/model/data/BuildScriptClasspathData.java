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
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class BuildScriptClasspathData extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<BuildScriptClasspathData> KEY =
    Key.create(BuildScriptClasspathData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);

  @Nullable
  private File gradleHomeDir;

  @NotNull
  private final List<ClasspathEntry> myClasspathEntries;


  public BuildScriptClasspathData(@NotNull ProjectSystemId owner, @NotNull List<ClasspathEntry> classpathEntries) {
    super(owner);
    myClasspathEntries = classpathEntries;
  }

  @Nullable
  public File getGradleHomeDir() {
    return gradleHomeDir;
  }

  public void setGradleHomeDir(@Nullable File gradleHomeDir) {
    this.gradleHomeDir = gradleHomeDir;
  }

  @NotNull
  public List<ClasspathEntry> getClasspathEntries() {
    return myClasspathEntries;
  }

  public static class ClasspathEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private final Set<String> myClassesFile;

    @NotNull
    private final Set<String> mySourcesFile;

    @NotNull
    private final Set<String> myJavadocFile;

    public ClasspathEntry(@NotNull Set<String> classesFile, @NotNull Set<String> sourcesFile, @NotNull Set<String> javadocFile) {
      myClassesFile = classesFile;
      mySourcesFile = sourcesFile;
      myJavadocFile = javadocFile;
    }

    @NotNull
    public Set<String> getClassesFile() {
      return myClassesFile;
    }

    @NotNull
    public Set<String> getSourcesFile() {
      return mySourcesFile;
    }

    @NotNull
    public Set<String> getJavadocFile() {
      return myJavadocFile;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ClasspathEntry)) return false;

      ClasspathEntry entry = (ClasspathEntry)o;

      if (!myClassesFile.equals(entry.myClassesFile)) return false;
      if (!myJavadocFile.equals(entry.myJavadocFile)) return false;
      if (!mySourcesFile.equals(entry.mySourcesFile)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myClassesFile.hashCode();
      result = 31 * result + mySourcesFile.hashCode();
      result = 31 * result + myJavadocFile.hashCode();
      return result;
    }
  }
}
