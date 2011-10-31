package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class LocalVariable extends Expression {
  private final Identifier myIdentifier;
  private final Type myType;
  private final Expression myInitializer;

  public LocalVariable(Identifier identifier, Type type, Expression initializer) {
    myIdentifier = identifier;
    myType = type;
    myInitializer = initializer;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myInitializer.toKotlin().isEmpty()) // TODO: remove
      return myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

    return myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      EQUAL + SPACE + myInitializer.toKotlin();
  }
}
