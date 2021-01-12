// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.openapi.compiler.CompileTask;
import com.intellij.serviceContainer.LazyExtensionInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nullable;

public final class CompileTaskBean extends LazyExtensionInstance<CompileTask> {
  public enum CompileTaskExecutionPhase { BEFORE, AFTER }

  @Attribute("execute")
  public CompileTaskExecutionPhase executionPhase = CompileTaskExecutionPhase.BEFORE;

  @Attribute("implementation")
  public String implementation;

  @Override
  protected @Nullable String getImplementationClassName() {
    return implementation;
  }
}
