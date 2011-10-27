package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class PrimitiveType extends Type {
  private Identifier myType;

  public PrimitiveType(Identifier type) {
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myType.toKotlin();
  }
}
