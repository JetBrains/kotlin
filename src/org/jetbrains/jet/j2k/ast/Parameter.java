package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Parameter extends Expression {
  private IdentifierImpl myIdentifier;
  private Type myType;

  public Parameter(IdentifierImpl identifier, Type type) {
    myIdentifier = identifier;
    myType = type;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();
  }
}
