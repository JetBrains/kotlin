package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class SynchronizedStatement extends Statement {
  private final Expression myExpression;
  private final Block myBlock;

  public SynchronizedStatement(Expression expression, Block block) {
    myExpression = expression;
    myBlock = block;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "synchronized" + SPACE + "(" + myExpression.toKotlin() + ")" + SPACE + myBlock.toKotlin();
  }
}
