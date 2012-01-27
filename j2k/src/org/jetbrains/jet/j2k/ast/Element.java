package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Element extends Node {
  @NotNull
  public static final Element EMPTY_ELEMENT = new EmptyElement();

  public boolean isEmpty() {
    return false;
  }

  /**
   * @author ignatov
   */
  private static class EmptyElement extends Element {
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
}
