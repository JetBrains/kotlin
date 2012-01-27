package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class AssertStatement extends Statement {
  private final Expression myCondition;
  private final Expression myDetail;

  public AssertStatement(Expression condition, Expression detail) {
    myCondition = condition;
    myDetail = detail;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String detail = myDetail != Expression.EMPTY_EXPRESSION ? "(" + myDetail.toKotlin() + ")" : EMPTY;
    return "assert" + detail + SPACE + "{" + myCondition.toKotlin() + "}";
  }
}
