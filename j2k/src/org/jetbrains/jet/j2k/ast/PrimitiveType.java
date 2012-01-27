package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class PrimitiveType extends Type {
  private final Identifier myType;

  public PrimitiveType(Identifier type) {
    myType = type;
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myType.toKotlin();
  }
}
