// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Registers text file type for particular virtual files rather than using .txt extension.
 * @author Rustam Vishnyakov
 */
public class EnforcedPlainTextFileTypeFactory {
  /**
   * @deprecated use {@link #getEnforcedPlainTextIcon()} instead 
   */
  @Deprecated public static final LayeredIcon ENFORCED_PLAIN_TEXT_ICON = new LayeredIcon();
  public static final String ENFORCED_PLAIN_TEXT = "Enforced Plain Text";

  private static final Icon ENFORCED_PLAIN_TEXT_LAZY_ICON = new IconLoader.LazyIcon() {
    @NotNull
    @Override
    protected Icon compute() {
      return new LayeredIcon(AllIcons.FileTypes.Text, PlatformIcons.EXCLUDED_FROM_COMPILE_ICON);
    }
  };

  @NotNull
  public static Icon getEnforcedPlainTextIcon() {
    return ENFORCED_PLAIN_TEXT_LAZY_ICON;
  }
}
