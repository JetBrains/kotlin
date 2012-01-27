package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class TypeCastExpression extends Expression {
  private final Type myType;
  private final Expression myExpression;

  public TypeCastExpression(Type type, Expression expression) {
    myType = type;
    myExpression = expression;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "(" + myExpression.toKotlin() + SPACE + "as" + SPACE + myType.toKotlin() + ")";
  }
}
