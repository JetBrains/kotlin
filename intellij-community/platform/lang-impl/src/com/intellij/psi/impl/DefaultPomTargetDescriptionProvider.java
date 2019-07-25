/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl;

import com.intellij.ide.TypePresentationService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomDescriptionProvider;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomTarget;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewNodeTextLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation;
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

    final String s = TypePresentationService.getService().getTypePresentableName(element.getClass());
    return s == null ? "Element" : s;
  }
}
