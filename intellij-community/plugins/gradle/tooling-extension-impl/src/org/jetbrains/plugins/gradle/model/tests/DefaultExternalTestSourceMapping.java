// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.tests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class DefaultExternalTestSourceMapping implements ExternalTestSourceMapping {
  @Nullable
  private String testName;
  @Nullable
  private String testTaskPath;
  @Nullable
  private String cleanTestTaskPath;
  @NotNull
  private Set<String> sourceFolders = Collections.emptySet();

  @Override
  @NotNull
  public Set<String> getSourceFolders() {
    return Collections.unmodifiableSet(sourceFolders);
  }

  public void setSourceFolders(@NotNull Set<String> sourceFolders) {
    this.sourceFolders = sourceFolders;
  }

  @NotNull
  @Override
  public String getTestName() {
    assert testName != null;
    return testName;
  }

  public void setTestName(@Nullable String testName) {
    this.testName = testName;
  }

  @Override
  @NotNull
  public String getTestTaskPath() {
    assert testTaskPath != null;
    return testTaskPath;
  }

  public void setTestTaskPath(@NotNull String testTaskPath) {
    this.testTaskPath = testTaskPath;
  }

  @NotNull
  @Override
  public String getCleanTestTaskPath() {
    assert cleanTestTaskPath != null;
    return cleanTestTaskPath;
  }

  public void setCleanTestTaskPath(@NotNull String cleanTestTaskPath) {
    this.cleanTestTaskPath = cleanTestTaskPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultExternalTestSourceMapping mapping = (DefaultExternalTestSourceMapping)o;

    if (testName != null ? !testName.equals(mapping.testName) : mapping.testName != null) return false;
    if (testTaskPath != null ? !testTaskPath.equals(mapping.testTaskPath) : mapping.testTaskPath != null) return false;
    if (cleanTestTaskPath != null ? !cleanTestTaskPath.equals(mapping.cleanTestTaskPath) : mapping.cleanTestTaskPath != null) return false;
    if (!sourceFolders.equals(mapping.sourceFolders)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = testName != null ? testName.hashCode() : 0;
    result = 31 * result + (testTaskPath != null ? testTaskPath.hashCode() : 0);
    result = 31 * result + (cleanTestTaskPath != null ? cleanTestTaskPath.hashCode() : 0);
    result = 31 * result + sourceFolders.hashCode();
    return result;
  }
}
