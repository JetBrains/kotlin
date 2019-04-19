// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

public class WordSelectioner extends AbstractWordSelectioner {
  private static final ExtensionPointName<Condition<PsiElement>> EP_NAME = ExtensionPointName.create("com.intellij.basicWordSelectionFilter");

  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    if (e instanceof PsiComment || e instanceof PsiWhiteSpace) {
      return false;
    }
    for (Condition<PsiElement> filter : EP_NAME.getExtensionList()) {
      if (!filter.value(e)) {
        return false;
      }
    }
    return true;
  }
}
