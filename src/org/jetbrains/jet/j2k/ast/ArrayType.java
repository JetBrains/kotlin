package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ArrayType extends Type {
  private final Type myType;

  public ArrayType(Type type) {
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (PRIMITIVE_TYPES.contains(myType.toKotlin().toLowerCase()))
      return myType.toKotlin() + "Array" + QUESTION; // TODO
    return "Array" + "<" + myType.toKotlin() + ">" + QUESTION;
  }
}
