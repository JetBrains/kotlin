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

  public static final class ClasspathEntry {
    @NotNull
    private final Set<String> classesFile;

    @NotNull
    private final Set<String> sourcesFile;

    @NotNull
    private final Set<String> javadocFile;

    @PropertyMapping({"classesFile", "sourcesFile", "javadocFile"})
    public ClasspathEntry(@NotNull Set<String> classesFile, @NotNull Set<String> sourcesFile, @NotNull Set<String> javadocFile) {
      this.classesFile = classesFile;
      this.sourcesFile = sourcesFile;
      this.javadocFile = javadocFile;
    }

    @NotNull
    public Set<String> getClassesFile() {
      return classesFile;
    }

    @NotNull
    public Set<String> getSourcesFile() {
      return sourcesFile;
    }

    @NotNull
    public Set<String> getJavadocFile() {
      return javadocFile;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ClasspathEntry)) return false;

      ClasspathEntry entry = (ClasspathEntry)o;

      if (!classesFile.equals(entry.classesFile)) return false;
      if (!javadocFile.equals(entry.javadocFile)) return false;
      if (!sourcesFile.equals(entry.sourcesFile)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = classesFile.hashCode();
      result = 31 * result + sourcesFile.hashCode();
      result = 31 * result + javadocFile.hashCode();
      return result;
    }
  }
}
