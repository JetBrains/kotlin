// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.MacroDirective;

public class MacroDirectiveImpl implements MacroDirective {
  private String name;
  private String value;

  @PropertyMapping({"name", "value"})
  public MacroDirectiveImpl(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public MacroDirectiveImpl(MacroDirective directive) {
    this(directive.getName(), directive.getValue());
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Nullable
  @Override
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
