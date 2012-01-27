package org.jetbrains.jet.j2k.ast;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Trait extends Class {
  public Trait(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
               List<Expression> baseClassParams, List<Type> implementsTypes, List<Member> members) {
    super(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, getMembers(members));
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