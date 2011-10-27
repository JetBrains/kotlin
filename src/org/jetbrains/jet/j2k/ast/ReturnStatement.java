package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ReturnStatement extends Statement {
  private Expression myExpression;

  public ReturnStatement(Expression expression) {
    myExpression = expression;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "return" + SPACE + myExpression.toKotlin();
  }
}
