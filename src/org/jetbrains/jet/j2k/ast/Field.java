package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Field extends Node {
  private Identifier myIdentifier;
  private Type myType;
  private Expression myInitializer;

  public Field(Identifier identifier, Type type, Expression initializer) {
    myIdentifier = identifier;
    myType = type;
    myInitializer = initializer;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String modifier = "var" + SPACE;

    if (myInitializer.toKotlin().isEmpty()) // TODO: remove
      return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

    return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      EQUAL + SPACE + myInitializer.toKotlin();
  }
}