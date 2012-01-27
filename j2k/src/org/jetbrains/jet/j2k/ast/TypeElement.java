package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class TypeElement extends Element {
  private final Type myType;

  public TypeElement(Type type) {
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myType.toKotlin();
  }
}
