// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ApiStatus.Internal
public interface CtrlMouseInfo {

  boolean isValid();

  @NotNull List<@NotNull TextRange> getRanges();

  default boolean isNavigatable() {
    return true;
  }

  @NotNull CtrlMouseDocInfo getDocInfo();
}
