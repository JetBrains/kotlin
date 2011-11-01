package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Type extends Element {
  protected boolean myNullable = true;

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.TYPE;
  }

  public void setNullable(boolean nullable) {
    myNullable = nullable;
  }

  public boolean isNullable() {
    return myNullable;
  }

  protected String isNullableStr() {
    return isNullable() ? QUESTION : EMPTY;
  }
}
