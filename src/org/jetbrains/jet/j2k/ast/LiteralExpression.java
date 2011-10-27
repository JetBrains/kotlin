package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class LiteralExpression extends Expression {
  private Identifier myIdentifier;

  public LiteralExpression(Identifier identifier) {
    myIdentifier = identifier;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myIdentifier.toKotlin();
  }
}
