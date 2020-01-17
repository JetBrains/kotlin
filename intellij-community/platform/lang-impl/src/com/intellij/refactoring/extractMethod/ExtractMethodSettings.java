// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.extractMethod;

import com.intellij.refactoring.util.AbstractVariableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExtractMethodSettings<T> {
  @NotNull
  String getMethodName();

  AbstractVariableData @NotNull [] getAbstractVariableData();

  @Nullable
  T getVisibility();
}