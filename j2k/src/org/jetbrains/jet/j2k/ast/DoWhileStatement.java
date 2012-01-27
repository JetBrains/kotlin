package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DoWhileStatement extends WhileStatement {
  public DoWhileStatement(Expression condition, Statement statement) {
    super(condition, statement);
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "do" + N +
      myStatement.toKotlin() + N +
      "while" + SPACE + "(" + myCondition.toKotlin() + ")";
  }
}