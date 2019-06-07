// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public interface GotoDeclarationHandler {
  ExtensionPointName<GotoDeclarationHandler> EP_NAME = ExtensionPointName.create("com.intellij.gotoDeclarationHandler");

  /**
   * Provides an array of target declarations for given {@code sourceElement}.
   *
   * @param sourceElement input PSI element
   * @param offset        offset in the file
   * @return all target declarations as an array of {@code PsiElement} or null if none were found
   */
  @Nullable
  PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor);

  /**
   * Provides the custom action text.
   *
   * @param context the action data context
   * @return the custom text or null to use the default text
   */
  @Nullable
  @Nls(capitalization = Nls.Capitalization.Title)
  default String getActionText(@NotNull DataContext context) {
    return null;
  }
}
