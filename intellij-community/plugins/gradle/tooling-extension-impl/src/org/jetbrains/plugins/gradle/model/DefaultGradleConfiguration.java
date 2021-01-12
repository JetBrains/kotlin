// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultGradleConfiguration implements GradleConfiguration {
  private static final long serialVersionUID = 1L;

  private final String name;
  private final String description;
  private final boolean visible;
  private final boolean scriptClasspathConfiguration;

  @PropertyMapping({"name", "description", "visible"})
  public DefaultGradleConfiguration(String name, String description, boolean visible) {
    this(name, description, visible, false);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultGradleConfiguration that = (DefaultGradleConfiguration)o;

    if (visible != that.visible) return false;
    if (scriptClasspathConfiguration != that.scriptClasspathConfiguration) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (visible ? 1 : 0);
    result = 31 * result + (scriptClasspathConfiguration ? 1 : 0);
    return result;
  }
}
