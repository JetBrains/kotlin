// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class EmptyPostfixTemplateProvider implements PostfixTemplateProvider {

  private final HashSet<PostfixTemplate> myTemplates = ContainerUtil.newHashSet();

  @NotNull
  @Override
  public String getId() {
    return "builtin.empty";
  }

  @NotNull
  @Override
  public Set<PostfixTemplate> getTemplates() {
    return myTemplates;
  }

  @Override
  public boolean isTerminalSymbol(char currentChar) {
    return false;
  }

  @Override
  public void preExpand(@NotNull PsiFile file, @NotNull Editor editor) {

  }

  @Override
  public void afterExpand(@NotNull PsiFile file, @NotNull Editor editor) {

  }

  @NotNull
  @Override
  public PsiFile preCheck(@NotNull PsiFile copyFile, @NotNull Editor realEditor, int currentOffset) {
    return copyFile;
  }
}
