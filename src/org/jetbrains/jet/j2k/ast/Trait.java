package org.jetbrains.jet.j2k.ast;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Trait extends Class {
  public Trait(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes, List<Expression> baseClassParams, List<Type> implementsTypes, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    super(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, innerClasses, methods, fields);
    TYPE = "trait";
  }

  @Override
  String primaryConstructorSignatureToKotlin() {
    return EMPTY;
  }

  boolean needOpenModifier() {
    return false;
  }
}