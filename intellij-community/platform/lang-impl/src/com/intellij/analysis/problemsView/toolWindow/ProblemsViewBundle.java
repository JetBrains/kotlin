// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

final class ProblemsViewBundle extends DynamicBundle {
  private static final @NonNls String BUNDLE = "messages.ProblemsViewBundle";
  private static final ProblemsViewBundle INSTANCE = new ProblemsViewBundle();

  private ProblemsViewBundle() {
    super(BUNDLE);
  }

  public static @NotNull String message(
    @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
    @NotNull Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static @NotNull Supplier<String> messagePointer(
    @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
    @NotNull Object @NotNull ... params) {
    return INSTANCE.getLazyMessage(key, params);
  }
}
