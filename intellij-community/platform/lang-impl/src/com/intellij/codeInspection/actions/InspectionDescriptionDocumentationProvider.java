
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.actions;

import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class InspectionDescriptionDocumentationProvider extends AbstractDocumentationProvider {
  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (!(element instanceof InspectionElement)) {
      return null;
    }

    InspectionToolWrapper toolWrapper = ((InspectionElement)element).getToolWrapper();
    return DocumentationMarkup.DEFINITION_START + StringUtil.escapeXmlEntities(toolWrapper.getDisplayName()) + DocumentationMarkup.DEFINITION_END +
           DocumentationMarkup.CONTENT_START + toolWrapper.loadDescription() +
           DocumentationMarkup.CONTENT_END;
  }
}
