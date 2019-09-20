// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author peter
 */
public abstract class ConsoleFolding {
  public static final ExtensionPointName<ConsoleFolding> EP_NAME = ExtensionPointName.create("com.intellij.console.folding");

  /**
   * @param project current project
   * @param line    line to check whether it should be folded or not
   * @return {@code true} if line should be folded, {@code false} if not
   */
  public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
    return shouldFoldLine(line);
  }

  /**
   * Return true if folded lines should not have dedicated line and should be attached to
   * the end of the line above instead
   */
  public boolean shouldBeAttachedToThePreviousLine() {
    return true;
  }

  /**
   * @param project current project
   * @param lines   lines to be folded
   * @return placeholder for lines or {@code null} if these lines should not be folded
   */
  @Nullable
  public String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
    return getPlaceholderText(lines);
  }

  /**
   * @param line to check if should be folded
   * @return {@code true} if line should be folded, {@code false} if not
   * @deprecated since 2018.1. Use {@link #shouldFoldLine(Project, String)} instead.
   */
  @Deprecated
  public boolean shouldFoldLine(@SuppressWarnings("unused") @NotNull String line) { return false; }

  /**
   * @param lines to fold
   * @return placeholder for lines
   * @deprecated since 2018.1. Use {@link #getPlaceholderText(Project, List)} instead.
   */
  @Deprecated
  @Nullable
  public String getPlaceholderText(@SuppressWarnings("unused") @NotNull List<String> lines) { return null; }
}
