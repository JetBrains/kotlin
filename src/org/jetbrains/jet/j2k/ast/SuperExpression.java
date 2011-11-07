package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class SuperExpression extends Expression {
  private Type myType;

  public SuperExpression(Type type) {
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "super" + "<" + myType.toKotlin() + ">";
  }
}
