package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * @author ignatov
 */
public class Field extends Node {
  final Identifier myIdentifier;
  private HashSet<String> myModifiers;
  private final Type myType;
  final Element myInitializer;

  public Field(Identifier identifier, HashSet<String> modifiers, Type type, Element initializer) {
    myIdentifier = identifier;
    myModifiers = modifiers;
    myType = type;
    myInitializer = initializer;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String modifier = (myModifiers.contains(Modifier.FINAL) ? "val" : "var") + SPACE;

    if (myInitializer.toKotlin().isEmpty()) // TODO: remove
      return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

    return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      EQUAL + SPACE + myInitializer.toKotlin();
  }
}