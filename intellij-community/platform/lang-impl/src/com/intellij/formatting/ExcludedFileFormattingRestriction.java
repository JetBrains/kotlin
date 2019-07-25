// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.LanguageFormattingRestriction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

public class ExcludedFileFormattingRestriction implements LanguageFormattingRestriction {
  @Override
  public boolean isFormatterAllowed(@NotNull PsiElement context) {
    try {
      PsiFile file = context.getContainingFile();
      CodeStyleSettings settings = CodeStyle.getSettings(file);
      return !settings.getExcludedFiles().contains(file);
    }
    catch (PsiInvalidElementAccessException e) {
      return false;
    }
  }
}
