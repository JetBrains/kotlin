package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author ignatov
 */
public class ClassVisitor extends JavaRecursiveElementVisitor {
  private Set<String> myClassIdentifiers;

  public ClassVisitor() {
    myClassIdentifiers = new HashSet<String>();
  }

  @NotNull
  public Set<String> getClassIdentifiers() {
    return new HashSet<String>(myClassIdentifiers);
  }

  @Override
  public void visitClass(@NotNull PsiClass aClass) {
    myClassIdentifiers.add(aClass.getQualifiedName());
    super.visitClass(aClass);
  }
}
