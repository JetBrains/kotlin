// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

public final class BuildScriptClasspathData extends AbstractExternalEntityData {
  @NotNull
  public static final Key<BuildScriptClasspathData> KEY =
    Key.create(BuildScriptClasspathData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);

  @Nullable
  private File gradleHomeDir;

  @NotNull
  private final List<ClasspathEntry> classpathEntries;

  @PropertyMapping({"owner", "classpathEntries"})
  public BuildScriptClasspathData(@NotNull ProjectSystemId owner, @NotNull List<ClasspathEntry> classpathEntries) {
    super(owner);

    this.classpathEntries = classpathEntries;
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
    return classpathEntries;
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
