package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Enum extends Class {
  public Enum(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes, List<Expression> baseClassParams, List<Type> implementsTypes, List<Class> innerClasses, List<Function> methods, List<Field> fields, List<Initializer> initializers) {
    super(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, innerClasses, methods, fields, initializers);
  }

    String primaryConstructorSignatureToKotlin() {
    String s = super.primaryConstructorSignatureToKotlin();
    return s.equals("()") ? EMPTY : s;
  }

  @Override
  boolean needOpenModifier() {
    return false;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return modifiersToKotlin() + "enum" + SPACE + myName.toKotlin() + primaryConstructorSignatureToKotlin() + typeParametersToKotlin() + implementTypesToKotlin() + SPACE + "{" + N +
      AstUtil.joinNodes(myFields, N) + N +
      AstUtil.joinNodes(methodsExceptConstructors(), N) + N +
      AstUtil.joinNodes(myInnerClasses, N) + N +
      primaryConstructorBodyToKotlin() + N +
      "}";
  }
}