// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.MacroDirective;

public class MacroDirectiveImpl implements MacroDirective {

  private String myName;
  private String myValue;

  public MacroDirectiveImpl(String name, String value) {
    myName = name;
    myValue = value;
  }

  public MacroDirectiveImpl(MacroDirective directive) {
    this(directive.getName(), directive.getValue());
  }

  @Override
  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  @Nullable
  @Override
  public String getValue() {
    return myValue;
  }

  public void setValue(String value) {
    myValue = value;
  }
}
