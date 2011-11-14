package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Parameter extends Expression {
  private final Identifier myIdentifier;
  private final Type myType;

  public Parameter(Identifier identifier, Type type) {
    myIdentifier = identifier;
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String vararg = myType.getKind() == Kind.VARARG ? "vararg" + SPACE : EMPTY;
    return vararg + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();
  }
}
