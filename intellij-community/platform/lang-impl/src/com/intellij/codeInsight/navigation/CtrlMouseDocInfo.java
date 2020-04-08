// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public final class CtrlMouseDocInfo {

  public static final CtrlMouseDocInfo EMPTY = new CtrlMouseDocInfo(null, null, null);

  public final @Nullable String text;
  final @Nullable PsiElement context;
  final @Nullable DocumentationProvider docProvider;

  public CtrlMouseDocInfo(@Nullable String text,
                          @Nullable PsiElement context,
                          @Nullable DocumentationProvider provider) {
    this.text = text;
    this.context = context;
    docProvider = provider;
  }
}
