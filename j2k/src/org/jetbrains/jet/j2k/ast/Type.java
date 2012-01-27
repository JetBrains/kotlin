package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Type extends Element {
  @NotNull
  public static final Type EMPTY_TYPE = new EmptyType();
  boolean myNullable = true;

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.TYPE;
  }

  @NotNull
  public Type convertedToNotNull() {
    myNullable = false;
    return this;
  }

  public boolean isNullable() {
    return myNullable;
  }

  String isNullableStr() {
    return isNullable() ? QUEST : EMPTY;
  }

  /**
   * @author ignatov
   */
  private static class EmptyType extends Type {
    @NotNull
    @Override
    public String toKotlin() {
      return "UNRESOLVED_TYPE";
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }
}
