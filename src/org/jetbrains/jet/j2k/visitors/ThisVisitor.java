package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * @author ignatov
 */
public class ThisVisitor extends JavaRecursiveElementVisitor {
  HashSet<PsiMethod> myResolvedConstructors = new HashSet<PsiMethod>();

  public HashSet<PsiMethod> getResult() {
    return myResolvedConstructors;
  }

  @Override
  public void visitReferenceExpression(PsiReferenceExpression expression) {
    for (PsiReference r : expression.getReferences())
      if (r.getCanonicalText().equals("this"))
        if (r.resolve() != null && r.resolve() instanceof PsiMethod && ((PsiMethod) r.resolve()).isConstructor())
          myResolvedConstructors.add((PsiMethod) r.resolve());
  }

  @Nullable
  public PsiMethod getPrimaryConstructor() {
    if (myResolvedConstructors.size() > 0) {
      PsiMethod first = myResolvedConstructors.toArray(new PsiMethod[myResolvedConstructors.size()])[0];
      for (PsiMethod m : myResolvedConstructors)
        if (m.hashCode() != first.hashCode())
          return null;
      return first;
    }
    return null;
  }
}
