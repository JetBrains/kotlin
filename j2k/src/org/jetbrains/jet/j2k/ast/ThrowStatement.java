package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ThrowStatement extends Expression {
  private final Expression myExpression;

  public ThrowStatement(Expression expression) {
    myExpression = expression;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "throw" + SPACE + myExpression.toKotlin();
  }
}
