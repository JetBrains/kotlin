package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class PrefixOperator extends Expression {
  private final String myOp;
  private final Expression myExpression;

  public PrefixOperator(String op, Expression expression) {
    myOp = op;
    myExpression = expression;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "(" + myOp + myExpression.toKotlin() + ")";
  }
}