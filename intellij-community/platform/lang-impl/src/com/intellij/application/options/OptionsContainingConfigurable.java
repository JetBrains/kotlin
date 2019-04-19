// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface OptionsContainingConfigurable {
  @NotNull
  Set<String> processListOptions();
}
