// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.target.java;

import com.intellij.openapi.options.BoundConfigurable;
import org.jetbrains.annotations.NotNull;

public class JavaLanguageRuntimeUIFactoryImpl implements JavaLanguageRuntimeUIFactory {
  @Override
  @NotNull
  public BoundConfigurable create(@NotNull JavaLanguageRuntimeConfiguration config) {
    return new JavaLanguageRuntimeUI(config);
  }
}