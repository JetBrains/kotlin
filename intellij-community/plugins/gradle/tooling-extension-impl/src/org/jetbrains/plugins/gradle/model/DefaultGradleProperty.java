// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleProperty implements GradleProperty {
  @NotNull
  private final String name;
  @NotNull
  private final String rootTypeFqn;

  @PropertyMapping({"name", "typeFqn"})
  public DefaultGradleProperty(@NotNull String name, @Nullable String typeFqn) {
    this.name = name;
    rootTypeFqn = typeFqn == null ? "Object" : typeFqn;
  }

  public DefaultGradleProperty(GradleProperty property) {
    this(property.getName(), property.getTypeFqn());
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultGradleProperty property = (DefaultGradleProperty)o;

    if (!name.equals(property.name)) return false;
    if (!rootTypeFqn.equals(property.rootTypeFqn)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + rootTypeFqn.hashCode();
    return result;
  }
}
