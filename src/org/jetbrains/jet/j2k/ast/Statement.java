package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Statement extends Element {
  public static Statement EMPTY_STATEMENT = new EmptyStatement();

  /**
   * @author ignatov
   */
  private static class EmptyStatement extends Statement {
    @NotNull
    @Override
    public String toKotlin() {
      return EMPTY;
    }
  }
}
