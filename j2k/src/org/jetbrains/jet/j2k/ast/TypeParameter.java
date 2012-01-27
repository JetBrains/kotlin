package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author ignatov
 */
public class TypeParameter extends Element {
  private final Identifier myName;
  private final List<Type> myExtendsTypes;

  public TypeParameter(Identifier name, List<Type> extendsTypes) {
    myName = name;
    myExtendsTypes = extendsTypes;
  }

  public boolean hasWhere() {
    return myExtendsTypes.size() > 1;
  }

  @NotNull
  public String getWhereToKotlin() {
    if (hasWhere())
      return myName.toKotlin() + SPACE + COLON + SPACE + myExtendsTypes.get(1).toKotlin();
    return EMPTY;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myExtendsTypes.size() > 0)
      return myName.toKotlin() + SPACE + COLON + SPACE + myExtendsTypes.get(0).toKotlin();
    return myName.toKotlin();
  }
}