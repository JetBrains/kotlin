package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DummyStringStatement extends Statement {
  private String myString;

  public DummyStringStatement(String string) {
    myString = string;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myString;
  }
}
