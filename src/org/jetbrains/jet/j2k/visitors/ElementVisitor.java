package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLocalVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.*;

import static org.jetbrains.jet.j2k.Converter.*;

/**
 * @author ignatov
 */
public class ElementVisitor extends JavaElementVisitor implements Visitor {
  private Element myResult = new EmptyElement();

  @Override
  public void visitLocalVariable(PsiLocalVariable variable) {
    super.visitLocalVariable(variable);
    myResult = new LocalVariable(
      new IdentifierImpl(variable.getName()), // TODO
      typeToType(variable.getType()),
      expressionToExpression(variable.getInitializer())
    );
  }

  @Override
  public void visitExpressionList(PsiExpressionList list) {
    super.visitExpressionList(list);
    myResult = new ExpressionList(
      expressionsToExpressionList(list.getExpressions()),
      typesToTypeList(list.getExpressionTypes())
    );

  }

  @NotNull
  public Element getResult() {
    return myResult;
  }
}
