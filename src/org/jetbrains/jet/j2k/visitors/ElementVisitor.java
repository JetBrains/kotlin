package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import static org.jetbrains.jet.j2k.Converter.*;

/**
 * @author ignatov
 */
public class ElementVisitor extends JavaElementVisitor {
  private Element myResult = Element.EMPTY_ELEMENT;

  @NotNull
  public Element getResult() {
    return myResult;
  }

  @Override
  public void visitLocalVariable(PsiLocalVariable variable) {
    super.visitLocalVariable(variable);

    myResult = new LocalVariable(
      new IdentifierImpl(variable.getName()), // TODO
      modifiersListToModifiersSet(variable.getModifierList()),
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

    if (!reference.isQualified())
      myResult = new IdentifierImpl(reference.getReferenceName());
    else {
      String result = reference.getReferenceName();
      PsiElement qualifier = reference.getQualifier();
      while (qualifier != null){
        final PsiJavaCodeReferenceElement p = (PsiJavaCodeReferenceElement) qualifier;
        result = p.getReferenceName() + "." + result; // TODO: maybe need to replace by safe call?
        qualifier = p.getQualifier();
      }
      myResult = new IdentifierImpl(result);
    }
  }

  @Override
  public void visitTypeElement(PsiTypeElement type) {
    super.visitTypeElement(type);
    myResult = new TypeElement(typeToType(type.getType()));
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