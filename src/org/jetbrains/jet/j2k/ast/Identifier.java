package org.jetbrains.jet.j2k.ast;

/**
 * @author ignatov
 */
public interface Identifier extends INode {
  public String getName();

  public boolean isEmpty();
}
