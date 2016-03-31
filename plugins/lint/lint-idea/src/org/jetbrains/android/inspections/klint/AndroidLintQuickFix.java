package org.jetbrains.android.inspections.lint;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene.Kudelevsky
 */
public interface AndroidLintQuickFix {
  AndroidLintQuickFix[] EMPTY_ARRAY = new AndroidLintQuickFix[0];
  
  void apply(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull AndroidQuickfixContexts.Context context);

  boolean isApplicable(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull AndroidQuickfixContexts.ContextType contextType);

  @NotNull
  String getName();
}
