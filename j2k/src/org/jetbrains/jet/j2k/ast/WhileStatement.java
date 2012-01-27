package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class WhileStatement extends Statement {
  final Expression myCondition;
  final Statement myStatement;

  public WhileStatement(Expression condition, Statement statement) {
    myCondition = condition;
    myStatement = statement;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "while" + SPACE + "(" + myCondition.toKotlin() + ")" + N +
      myStatement.toKotlin();
  }
}
