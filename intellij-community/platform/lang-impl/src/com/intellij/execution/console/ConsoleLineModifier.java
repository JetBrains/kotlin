// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface ConsoleLineModifier {
  ExtensionPointName<ConsoleLineModifier> EP_NAME = ExtensionPointName.create("com.intellij.stacktrace.fold.line.modifier");

  /**
   * @param line initial console line
   * @return     modified text to match existing folding patterns,
   *             e.g. remove module name from modularized stacktrace line so old patterns would work in modularized java environment
   */
  @Nullable
  String modify(@NotNull String line);
}
