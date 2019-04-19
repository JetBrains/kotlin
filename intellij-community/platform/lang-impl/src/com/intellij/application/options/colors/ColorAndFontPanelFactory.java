// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.colors;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;

/**
 * Generalises {@link ColorSettingsPage} in a way that allows to provide custom {@link PreviewPanel preview panel}.
 * 
 * @author yole
 */
public interface ColorAndFontPanelFactory {

  ExtensionPointName<ColorAndFontPanelFactory> EP_NAME = ExtensionPointName.create("com.intellij.colorAndFontPanelFactory");
  
  @NotNull
  NewColorAndFontPanel createPanel(@NotNull ColorAndFontOptions options);
  
  @NotNull
  String getPanelDisplayName();

  /**
   * @see {@link com.intellij.openapi.options.SearchableConfigurable#getOriginalClass()}
   */
  @NotNull
  default Class<?> getOriginalClass() {
    return this.getClass();
  }
}
