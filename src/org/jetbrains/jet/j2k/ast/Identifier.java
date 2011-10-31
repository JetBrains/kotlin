package org.jetbrains.jet.j2k.ast;

/**
 * @author ignatov
 */
public interface Identifier extends INode {
  public static Identifier EMPTY_IDENTIFIER = new IdentifierImpl("");

  public String getName();

  public boolean isEmpty();
}
