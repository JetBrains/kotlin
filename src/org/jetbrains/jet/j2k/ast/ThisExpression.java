package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ThisExpression extends Expression {
  private Identifier myIdentifier;

  public ThisExpression(Identifier identifier) {
    myIdentifier = identifier;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myIdentifier.isEmpty())
      return "this";
    return "this" + AT + myIdentifier.toKotlin();
  }
}
