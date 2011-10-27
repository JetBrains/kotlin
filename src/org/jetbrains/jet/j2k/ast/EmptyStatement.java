package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class EmptyStatement extends Statement {
  @NotNull
  @Override
  public String toKotlin() {
    return EMPTY;
  }
}
