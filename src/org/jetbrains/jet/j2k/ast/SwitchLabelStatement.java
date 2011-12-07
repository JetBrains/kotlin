package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class SwitchLabelStatement extends Statement {
  private Expression myExpression;

  public SwitchLabelStatement(final Expression expression) {
    myExpression = expression;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myExpression.toKotlin();
  }
}
