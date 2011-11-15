package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Element extends Node {
  public static final Element EMPTY_ELEMENT = new EmptyElement();

  /**
   * @author ignatov
   */
  private static class EmptyElement extends Element {
    @NotNull
    @Override
    public String toKotlin() {
      return EMPTY;
    }
  }
}
