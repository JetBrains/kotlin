package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class SureCallChainExpression extends Expression {
  private final Expression myExpression;
  private final String myConversion;

  public SureCallChainExpression(Expression expression, String conversion) {
    myExpression = expression;
    myConversion = conversion;
  }

  @Override
  public boolean isEmpty() {
    return myExpression.isEmpty();
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myExpression.toKotlin() + myConversion;
  }
}
