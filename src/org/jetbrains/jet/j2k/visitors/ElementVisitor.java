package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import java.util.List;

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
      typeToType(variable.getType(), Converter.isNotNull(variable.getModifierList())),
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

    final List<Type> types = typesToTypeList(reference.getTypeParameters());
    if (!reference.isQualified()) {
      myResult = new ReferenceElement(
        new IdentifierImpl(reference.getReferenceName()),
        types
      );
    } else {
      String result = new IdentifierImpl(reference.getReferenceName()).toKotlin();
      PsiElement qualifier = reference.getQualifier();
      while (qualifier != null) {
        final PsiJavaCodeReferenceElement p = (PsiJavaCodeReferenceElement) qualifier;
        result = new IdentifierImpl(p.getReferenceName()).toKotlin() + "." + result; // TODO: maybe need to replace by safe call?
        qualifier = p.getQualifier();
      }
      myResult = new ReferenceElement(
        new IdentifierImpl(result),
        types
      );
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
      parametersToParameterList(list.getParameters())
    );
  }
}