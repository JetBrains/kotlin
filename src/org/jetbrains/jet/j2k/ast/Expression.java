package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Expression extends Statement {
  public static final Expression EMPTY_EXPRESSION = new EmptyExpression();

  /**
   * @author ignatov
   */
  private static class EmptyExpression extends Expression {
    @NotNull
    @Override
    public String toKotlin() {
      return EMPTY;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }
  }

  boolean isNullable() {
    return false;
  }
}
