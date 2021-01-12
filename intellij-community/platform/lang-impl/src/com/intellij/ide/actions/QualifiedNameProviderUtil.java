// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class QualifiedNameProviderUtil {
  private QualifiedNameProviderUtil() {}

  @Nullable
  public static PsiElement adjustElementToCopy(@NotNull PsiElement element) {
    for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensionList()) {
      PsiElement adjustedElement = provider.adjustElementToCopy(element);
      if (adjustedElement != null) return adjustedElement;
    }
    return null;
  }

  @Nullable
  public static String getQualifiedName(@NotNull PsiElement element) {
    for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensionList()) {
      String qualifiedName = provider.getQualifiedName(element);
      if (qualifiedName != null) return qualifiedName;
    }
    return null;
  }

  @Nullable
  public static PsiElement qualifiedNameToElement(@NotNull String qualifiedName, @NotNull Project project) {
    for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensionList()) {
      PsiElement element = provider.qualifiedNameToElement(qualifiedName, project);
      if (element != null) return element;
    }
    return null;
  }
}
