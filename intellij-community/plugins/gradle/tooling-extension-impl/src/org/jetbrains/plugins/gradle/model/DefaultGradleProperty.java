// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleProperty implements GradleProperty {
  @NotNull
  private final String name;
  @NotNull
  private final String rootTypeFqn;
  @Nullable
  private final Serializable value;

  public DefaultGradleProperty(@NotNull String name, @Nullable String typeFqn, @Nullable Serializable value) {
    this.name = name;
    rootTypeFqn = typeFqn == null ? "Object" : typeFqn;
    this.value = value;
  }

  @SuppressWarnings("unused")
  protected DefaultGradleProperty() {
    this.name = "";
    rootTypeFqn = "Object";
    this.value = null;
  }

  public DefaultGradleProperty(GradleProperty property) {
    this(property.getName(), property.getTypeFqn(), property.getValue());
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @NotNull
  @Override
  public String getTypeFqn() {
    return rootTypeFqn;
  }

  @Nullable
  @Override
  public Serializable getValue() {
    return value;
  }
}
