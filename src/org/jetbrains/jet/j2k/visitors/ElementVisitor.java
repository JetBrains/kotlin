package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import java.util.HashSet;

import static org.jetbrains.jet.j2k.Converter.*;

/**
 * @author ignatov
 */
public class ElementVisitor extends JavaElementVisitor implements Visitor {
  private Element myResult = Element.EMPTY_ELEMENT;

  @NotNull
  @Override
  public Element getResult() {
    return myResult;
  }

  @Override
  public void visitLocalVariable(PsiLocalVariable variable) {
    super.visitLocalVariable(variable);

    myResult = new LocalVariable(
      new IdentifierImpl(variable.getName()), // TODO
      getModifiersSet(variable.getModifierList()),
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

  @Override
  public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
    super.visitReferenceElement(reference);
    myResult = new IdentifierImpl(reference.getQualifiedName()); // TODO
  }

  @Override
  public void visitTypeParameter(PsiTypeParameter classParameter) {
    super.visitTypeParameter(classParameter);
    myResult = new TypeParameter(
      new IdentifierImpl(classParameter.getName()), // TODO
      typesToTypeList(classParameter.getExtendsListTypes())
    );
  }

  @Override
  public void visitParameterList(PsiParameterList list) {
    super.visitParameterList(list);
    myResult = new ParameterList(
      Converter.parametersToParameterList(list.getParameters())
    );
  }
}