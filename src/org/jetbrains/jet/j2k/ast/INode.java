package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public interface INode {
  @NotNull
  public String toKotlin();

  @NotNull
  public Kind getKind();

  public enum Kind {
    UNDEFINED, TYPE, CONSTRUCTOR,
  }
}
