package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class InProjectionType extends Type {
  private final Type myBound;

  public InProjectionType(Type bound) {
    myBound = bound;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "in" + SPACE + myBound.toKotlin();
  }
}
