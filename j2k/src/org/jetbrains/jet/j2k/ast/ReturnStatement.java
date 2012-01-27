package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

/**
 * @author ignatov
 */
public class ReturnStatement extends Statement {
  private final Expression myExpression;
  private String myConversion = EMPTY;

  public ReturnStatement(Expression expression) {
    myExpression = expression;
  }

  public ReturnStatement(final Expression expression, final String conversion) {
    this(expression);
    myConversion = conversion;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "return" + SPACE + AstUtil.applyConversionForOneItem(myExpression.toKotlin(), myConversion);
  }
}
