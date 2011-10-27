package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class EmptyExpression extends Expression {
  @NotNull
  @Override
  public String toKotlin() {
    return EMPTY;
  }
}
