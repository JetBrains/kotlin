package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class IsOperator extends Expression {
  private Expression myExpression;
  private Element myTypeElement;

  public IsOperator(Expression expression, Element typeElement) {
    myExpression = expression;
    myTypeElement = typeElement;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "(" + myExpression.toKotlin() + SPACE + "is" + SPACE + myTypeElement.toKotlin() + ")";
  }
}
