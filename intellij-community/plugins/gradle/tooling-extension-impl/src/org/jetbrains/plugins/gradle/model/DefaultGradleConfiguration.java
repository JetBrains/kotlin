// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleConfiguration implements GradleConfiguration {
  private static final long serialVersionUID = 1L;

  private final String name;
  private final String description;
  private final boolean visible;
  private final boolean scriptClasspathConfiguration;

  public DefaultGradleConfiguration(String name, String description, boolean visible) {
    this(name, description, visible, false);
  }

  @SuppressWarnings("unused")
  private DefaultGradleConfiguration() {
    name = "";
    description = "";
    visible = false;
    scriptClasspathConfiguration = false;
  }

  public DefaultGradleConfiguration(@NotNull String name, @Nullable String description, boolean visible, boolean scriptClasspathConfiguration) {
    this.name = name;
    this.description = description;
    this.visible = visible;
    this.scriptClasspathConfiguration = scriptClasspathConfiguration;
  }

  public DefaultGradleConfiguration(GradleConfiguration configuration) {
    this(configuration.getName(), configuration.getDescription(), configuration.isVisible(),
         configuration.isScriptClasspathConfiguration());
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public boolean isScriptClasspathConfiguration() {
    return scriptClasspathConfiguration;
  }
}
