// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class SingleTargetElementInfo extends BaseCtrlMouseInfo {

  private final @NotNull PsiElement myElementAtPointer;
  private final @NotNull PsiElement myTargetElement;

  public SingleTargetElementInfo(@NotNull PsiElement elementAtPointer, @NotNull PsiElement targetElement) {
    super(elementAtPointer);
    myElementAtPointer = elementAtPointer;
    myTargetElement = targetElement;
  }

  public SingleTargetElementInfo(@NotNull PsiReference reference, @NotNull PsiElement targetElement) {
    super(ReferenceRange.getAbsoluteRanges(reference));
    myElementAtPointer = reference.getElement();
    myTargetElement = targetElement;
  }

  @Override
  public @NotNull CtrlMouseDocInfo getDocInfo() {
    return isValid() ? generateInfo(myTargetElement, myElementAtPointer, isNavigatable()) : CtrlMouseDocInfo.EMPTY;
  }

  @Override
  public boolean isValid() {
    return myTargetElement.isValid() && myElementAtPointer.isValid();
  }

  @Override
  public boolean isNavigatable() {
    return myTargetElement != myElementAtPointer && myTargetElement != myElementAtPointer.getParent();
  }

  @ApiStatus.Internal
  public static @NotNull CtrlMouseDocInfo generateInfo(PsiElement element, PsiElement atPointer, boolean fallbackToBasicInfo) {
    final DocumentationProvider documentationProvider = DocumentationManager.getProviderFromElement(element, atPointer);
    String result = documentationProvider.getQuickNavigateInfo(element, atPointer);
    if (result == null && fallbackToBasicInfo) {
      result = doGenerateInfo(element);
    }
    return result == null ? CtrlMouseDocInfo.EMPTY : new CtrlMouseDocInfo(result, atPointer, documentationProvider);
  }

  @Nullable
  private static String doGenerateInfo(@NotNull PsiElement element) {
    if (element instanceof PsiFile) {
      final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null) {
        return virtualFile.getPresentableUrl();
      }
    }

    String info = getQuickNavigateInfo(element);
    if (info != null) {
      return info;
    }

    if (element instanceof NavigationItem) {
      final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
      if (presentation != null) {
        return presentation.getPresentableText();
      }
    }

    return null;
  }

  @Nullable
  private static String getQuickNavigateInfo(PsiElement element) {
    final String name = ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE);
    if (StringUtil.isEmpty(name)) return null;
    final String typeName = ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE);
    final PsiFile file = element.getContainingFile();
    final StringBuilder sb = new StringBuilder();
    if (StringUtil.isNotEmpty(typeName)) sb.append(typeName).append(" ");
    sb.append("\"").append(name).append("\"");
    if (file != null && file.isPhysical()) {
      sb.append(" [").append(file.getName()).append("]");
    }
    return sb.toString();
  }
}
