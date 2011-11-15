package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;

import java.util.HashSet;


/**
 * @author ignatov
 */
public class SuperVisitor extends JavaRecursiveElementVisitor {
  private final HashSet<PsiExpressionList> myResolvedSuperCallParameters;

  public SuperVisitor() {
    myResolvedSuperCallParameters = new HashSet<PsiExpressionList>();
  }

  public HashSet<PsiExpressionList> getResolvedSuperCallParameters() {
    return myResolvedSuperCallParameters;
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    super.visitMethodCallExpression(expression);
    if (isSuper(expression.getMethodExpression()))
      myResolvedSuperCallParameters.add(expression.getArgumentList());
  }

  static boolean isSuper(PsiReference r) {
    if (r.getCanonicalText().equals("super")) {
      final PsiElement baseConstructor = r.resolve();
      if (baseConstructor != null && baseConstructor instanceof PsiMethod && ((PsiMethod) baseConstructor).isConstructor()) {
        return true;
      }
    }
    return false;
  }
}
