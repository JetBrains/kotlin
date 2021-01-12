// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.hint;

import com.intellij.openapi.util.MixinExtension;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeclarationRangeUtil {
  @NotNull
  public static TextRange getDeclarationRange(@NotNull PsiElement container) {
    TextRange textRange = getPossibleDeclarationAtRange(container);
    assert textRange != null : "Declaration range is invalid for " + container.getClass();
    return textRange;
  }

  @Nullable
  public static TextRange getPossibleDeclarationAtRange(@NotNull PsiElement container) {
    DeclarationRangeHandler handler = MixinExtension.getInstance(DeclarationRangeHandler.EP_NAME, container);
    //noinspection unchecked
    return handler != null ? handler.getDeclarationRange(container) : null;
  }
}