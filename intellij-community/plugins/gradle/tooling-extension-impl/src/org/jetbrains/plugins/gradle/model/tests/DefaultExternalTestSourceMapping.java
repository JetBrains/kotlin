// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.tests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultExternalTestSourceMapping implements ExternalTestSourceMapping {

  @Nullable
  private String testName = null;

  @Nullable
  private String testTaskPath = null;

  @Nullable
  private String cleanTestTaskPath = null;

  @NotNull
  private Set<String> sourceFolders = Collections.emptySet();

  public DefaultExternalTestSourceMapping() { }

  public DefaultExternalTestSourceMapping(@NotNull ExternalTestSourceMapping externalTestSourceMapping) {
    testName = externalTestSourceMapping.getTestName();
    testTaskPath = externalTestSourceMapping.getTestTaskPath();
    cleanTestTaskPath = externalTestSourceMapping.getCleanTestTaskPath();
    sourceFolders = new LinkedHashSet<String>(externalTestSourceMapping.getSourceFolders());
  }

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
}
