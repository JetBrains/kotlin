package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Enum extends Class {
  public Enum(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
              List<Expression> baseClassParams, List<Type> implementsTypes, List<Member> members) {
    super(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, getMembers(members));
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
    return modifiersToKotlin() + "enum class" + SPACE + myName.toKotlin() + primaryConstructorSignatureToKotlin() +
      typeParametersToKotlin() + implementTypesToKotlin() + SPACE + "{" + N +
      AstUtil.joinNodes(membersExceptConstructors(), N) + N +
      primaryConstructorBodyToKotlin() + N +
      "public fun name()  : String { return \"\" }" + N + // TODO : remove hack
      "public fun order() : Int { return 0 }" + N +
      "}";
  }
}