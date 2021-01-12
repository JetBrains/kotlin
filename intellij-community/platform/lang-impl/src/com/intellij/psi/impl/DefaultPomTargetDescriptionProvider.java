// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl;

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation;
import com.intellij.ide.TypePresentationService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomDescriptionProvider;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomTarget;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewNodeTextLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class DefaultPomTargetDescriptionProvider extends PomDescriptionProvider {
  @Override
  public String getElementDescription(@NotNull PomTarget element, @NotNull ElementDescriptionLocation location) {
    if (element instanceof PsiElement) return null;
    
    if (location == UsageViewTypeLocation.INSTANCE) {
      return getTypeName(element);
    }
    if (location == UsageViewNodeTextLocation.INSTANCE) {
      return getTypeName(element) + " " + StringUtil.notNullize(element instanceof PomNamedTarget ? ((PomNamedTarget)element).getName() : null, "''");
    }
    if (location instanceof HighlightUsagesDescriptionLocation) {
      return getTypeName(element);
    }
    return null;
  }

  private static String getTypeName(PomTarget element) {
    TypePresentationService presentationService = TypePresentationService.getService();
    String elementTypeName = presentationService.getTypeName(element);
    if (elementTypeName != null) return elementTypeName;

    String classTypeName = presentationService.getTypePresentableName(element.getClass());
    return classTypeName == null ? "Element" : classTypeName;
  }
}
