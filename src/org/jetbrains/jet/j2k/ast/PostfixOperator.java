package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class PostfixOperator extends Expression {
  private final String myOp;
  private final Expression myExpression;

  public PostfixOperator(String op, Expression expression) {
    myOp = op;
    myExpression = expression;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "(" + myExpression.toKotlin() + myOp + ")";
  }
}