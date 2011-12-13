package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class LiteralExpression extends Expression {
  private final Identifier myIdentifier;

  public LiteralExpression(Identifier identifier) {
    myIdentifier = identifier;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.LITERAL;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myIdentifier.toKotlin();
  }
}
