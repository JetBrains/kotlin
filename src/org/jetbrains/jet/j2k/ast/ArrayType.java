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

  public Type getInnerType() {
    return myType;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.ARRAY_TYPE;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (PRIMITIVE_TYPES.contains(myType.toKotlin().toLowerCase()))
      return myType.toKotlin() + "Array" + isNullableStr(); // returns IntArray, BooleanArray, etc.
    return "Array" + "<" + myType.toKotlin() + ">" + isNullableStr();
  }
}
