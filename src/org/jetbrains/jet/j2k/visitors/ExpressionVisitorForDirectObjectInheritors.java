package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.DummyMethodCallExpression;
import org.jetbrains.jet.j2k.ast.DummyStringExpression;
import org.jetbrains.jet.j2k.ast.IdentifierImpl;

import static org.jetbrains.jet.j2k.visitors.TypeVisitor.JAVA_LANG_OBJECT;

/**
 * @author ignatov
 */
public class ExpressionVisitorForDirectObjectInheritors extends ExpressionVisitor {
  @Override
  public void visitMethodCallExpression(@NotNull final PsiMethodCallExpression expression) {
    if (superMethodInvocation(expression.getMethodExpression(), "hashCode"))
      myResult = new DummyMethodCallExpression(new IdentifierImpl("System"), "identityHashCode", new IdentifierImpl("this"));
    else if (superMethodInvocation(expression.getMethodExpression(), "equals"))
      myResult = new DummyMethodCallExpression(new IdentifierImpl("this"), "identityEquals", Converter.elementToElement(expression.getArgumentList()));
    else if (superMethodInvocation(expression.getMethodExpression(), "toString"))
      myResult = new DummyStringExpression(String.format("getJavaClass<%s>.getName() + '@' + Integer.toHexString(hashCode())", getClassName(expression.getMethodExpression())));
    else
      super.visitMethodCallExpression(expression);
  }

  @Override
  public void visitReferenceExpression(@NotNull final PsiReferenceExpression expression) {
    super.visitReferenceExpression(expression);
  }

  private static boolean superMethodInvocation(@NotNull final PsiReferenceExpression expression, final String methodName) {
    String referenceName = expression.getReferenceName();
    PsiExpression qualifierExpression = expression.getQualifierExpression();
    if (referenceName != null && referenceName.equals(methodName)) {
      if (qualifierExpression instanceof PsiSuperExpression) {
        PsiType type = qualifierExpression.getType();
        if (type != null && type.getCanonicalText().equals(JAVA_LANG_OBJECT))
          return true;
      }
    }
    return false;
  }
}
