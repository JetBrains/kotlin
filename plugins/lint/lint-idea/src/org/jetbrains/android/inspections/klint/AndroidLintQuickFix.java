package org.jetbrains.android.inspections.klint;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface AndroidLintQuickFix {
  AndroidLintQuickFix[] EMPTY_ARRAY = new AndroidLintQuickFix[0];
  
  void apply(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull AndroidQuickfixContexts.Context context);

  boolean isApplicable(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull AndroidQuickfixContexts.ContextType contextType);

  @NotNull
  String getName();
}

