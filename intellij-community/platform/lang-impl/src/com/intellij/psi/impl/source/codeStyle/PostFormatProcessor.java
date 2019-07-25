// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

public interface PostFormatProcessor {
  ExtensionPointName<PostFormatProcessor> EP_NAME = ExtensionPointName.create("com.intellij.postFormatProcessor");

  @NotNull
  PsiElement processElement(@NotNull PsiElement source, @NotNull CodeStyleSettings settings);

  @NotNull
  TextRange processText(@NotNull PsiFile source, @NotNull TextRange rangeToReformat, @NotNull CodeStyleSettings settings);
}
