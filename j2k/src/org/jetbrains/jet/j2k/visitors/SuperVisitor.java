package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;


/**
 * @author ignatov
 */
public class SuperVisitor extends JavaRecursiveElementVisitor {
  @NotNull
  private final HashSet<PsiExpressionList> myResolvedSuperCallParameters;

  public SuperVisitor() {
    myResolvedSuperCallParameters = new HashSet<PsiExpressionList>();
  }

  @NotNull
  public HashSet<PsiExpressionList> getResolvedSuperCallParameters() {
    return myResolvedSuperCallParameters;
  }

  @Override
  public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
    super.visitMethodCallExpression(expression);
    if (isSuper(expression.getMethodExpression()))
      myResolvedSuperCallParameters.add(expression.getArgumentList());
  }

  static boolean isSuper(@NotNull PsiReference r) {
    if (r.getCanonicalText().equals("super")) {
      final PsiElement baseConstructor = r.resolve();
      if (baseConstructor != null && baseConstructor instanceof PsiMethod && ((PsiMethod) baseConstructor).isConstructor()) {
        return true;
      }
    }
    return false;
  }
}
