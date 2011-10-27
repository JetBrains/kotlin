package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ClassType extends Type {
  private Identifier myType;

  public ClassType(Identifier type) {
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myType.toKotlin() + QUESTION;
  }
}
