// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingConfig;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingModel;

import java.io.Serializable;
import java.util.Map;

public class AnnotationProcessingModelImpl implements AnnotationProcessingModel, Serializable {
  private final Map<String, AnnotationProcessingConfig> configs;

  public AnnotationProcessingModelImpl(Map<String, AnnotationProcessingConfig> configs) {
    this.configs = configs;
  }

  @NotNull
  @Override
  public Map<String, AnnotationProcessingConfig> allConfigs() {
    return configs;
  }

  @Nullable
  @Override
  public AnnotationProcessingConfig bySourceSetName(@NotNull String sourceSetName) {
    return configs.get(sourceSetName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AnnotationProcessingModelImpl model = (AnnotationProcessingModelImpl)o;
    return Objects.equal(configs, model.configs);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(configs);
  }
}
