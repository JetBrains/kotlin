package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public interface Identifier extends INode {
  @NotNull
  public static Identifier EMPTY_IDENTIFIER = new IdentifierImpl("");

  public boolean isEmpty();

  String getName();
}
