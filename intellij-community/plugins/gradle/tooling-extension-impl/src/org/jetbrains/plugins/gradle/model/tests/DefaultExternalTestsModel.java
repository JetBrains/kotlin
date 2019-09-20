// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.tests;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class DefaultExternalTestsModel implements ExternalTestsModel {
  @NotNull
  private List<ExternalTestSourceMapping> sourceTestMappings = Collections.emptyList();

  @Override
  @NotNull
  public List<ExternalTestSourceMapping> getTestSourceMappings() {
    return Collections.unmodifiableList(sourceTestMappings);
  }

  public void setSourceTestMappings(@NotNull List<ExternalTestSourceMapping> sourceTestMappings) {
    this.sourceTestMappings = sourceTestMappings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DefaultExternalTestsModel model = (DefaultExternalTestsModel)o;
    if (!sourceTestMappings.equals(model.sourceTestMappings)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return sourceTestMappings.hashCode();
  }
}
