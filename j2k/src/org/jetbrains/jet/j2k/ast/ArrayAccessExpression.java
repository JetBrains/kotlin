package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ArrayAccessExpression extends Expression {
  private final Expression myExpression;
  private final Expression myIndex;

  public ArrayAccessExpression(Expression expression, Expression index) {
    myExpression = expression;
    myIndex = index;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myExpression.toKotlin() + "[" + myIndex.toKotlin() + "]";
  }
}
