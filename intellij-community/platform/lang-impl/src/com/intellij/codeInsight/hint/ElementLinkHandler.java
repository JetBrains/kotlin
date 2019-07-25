// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.highlighting.TooltipLinkHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.actions.QualifiedNameProviderUtil.qualifiedNameToElement;

/**
 * Handles tooltip links in format {@code #element/qualified.name}.
 * On a click opens specified element in an editor and positions caret to the corresponding offset.
 */
public final class ElementLinkHandler extends TooltipLinkHandler {
  @Override
  public boolean handleLink(@NotNull String name, @NotNull Editor editor) {
    Project project = editor.getProject();
    if (project != null) {
      PsiElement element = qualifiedNameToElement(name, project);
      if (element instanceof Navigatable) {
        Navigatable navigatable = (Navigatable)element;
        if (navigatable.canNavigate()) {
          navigatable.navigate(true);
          return true;
        }
      }
    }
    return false;
  }
}
