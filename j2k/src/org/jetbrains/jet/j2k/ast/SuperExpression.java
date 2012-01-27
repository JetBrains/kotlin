package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class SuperExpression extends Expression {
  private final Identifier myIdentifier;

  public SuperExpression(Identifier identifier) {
    myIdentifier = identifier;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myIdentifier.isEmpty())
      return "super";
    return "super" + AT + myIdentifier.toKotlin();
  }
}
