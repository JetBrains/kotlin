package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class OutProjectionType extends Type {
  private final Type myBound;

  public OutProjectionType(Type bound) {
    myBound = bound;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "out" + SPACE + myBound.toKotlin();
  }
}
