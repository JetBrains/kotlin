package org.jetbrains.jet.j2k.ast;

/**
 * @author ignatov
 */
public interface IMember extends INode {
  boolean isStatic();

  boolean isAbstract();
}
