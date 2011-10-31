package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class BinaryExpression extends Expression {
  private final Expression myLeft;
  private final Expression myRight;
  private final String myOp;

  public BinaryExpression(Expression left, Expression right, String op) {
    myLeft = left;
    myRight = right;
    myOp = op;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "(" + myLeft.toKotlin() + SPACE + myOp + SPACE + myRight.toKotlin() + ")";
  }
}
