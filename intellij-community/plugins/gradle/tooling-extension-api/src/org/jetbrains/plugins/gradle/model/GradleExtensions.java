// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface GradleExtensions extends Serializable {
  @Nullable
  String getParentProjectPath();

  @NotNull
  List<? extends GradleExtension> getExtensions();

  @NotNull
  List<? extends GradleConvention> getConventions();

  @NotNull
  List<? extends GradleProperty> getGradleProperties();

  @NotNull
  List<? extends ExternalTask> getTasks();

  @NotNull
  List<? extends GradleConfiguration> getConfigurations();
}
