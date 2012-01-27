package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Parameter extends Expression {
  private final Identifier myIdentifier;
  private final Type myType;
  private boolean myReadOnly;

  public Parameter(Identifier identifier, Type type) {
    myIdentifier = identifier;
    myType = type;
    myReadOnly = true;
  }

  public Parameter(IdentifierImpl identifier, Type type, boolean readOnly) {
    this(identifier, type);
    myReadOnly = readOnly;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String vararg = myType.getKind() == Kind.VARARG ? "vararg" + SPACE : EMPTY;
    String var = myReadOnly ? EMPTY : "var" + SPACE;
    return vararg + var + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();
  }
}
