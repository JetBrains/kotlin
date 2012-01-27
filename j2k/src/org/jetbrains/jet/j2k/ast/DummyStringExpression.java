package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DummyStringExpression extends Expression {
  private final String myString;

  public DummyStringExpression(String string) {
    myString = string;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myString;
  }
}
