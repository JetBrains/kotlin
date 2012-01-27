package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public interface Identifier extends INode {
  @NotNull
  Identifier EMPTY_IDENTIFIER = new IdentifierImpl("");

  boolean isEmpty();

  String getName();
}
