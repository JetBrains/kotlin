// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.openapi.preview.PreviewPanelProvider;
import com.intellij.openapi.preview.PreviewProviderId;
import com.intellij.openapi.util.Couple;
import com.intellij.psi.PsiElement;

@Deprecated
public final class DocumentationPreviewPanelProvider extends PreviewPanelProvider<Couple<PsiElement>, DocumentationComponent> {
  public static final PreviewProviderId<Couple<PsiElement>, DocumentationComponent> ID = null;

  private DocumentationPreviewPanelProvider() {
  }
}
