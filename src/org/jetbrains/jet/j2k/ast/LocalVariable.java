package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * @author ignatov
 */
public class LocalVariable extends Expression {
  private final Identifier myIdentifier;
  private HashSet<String> myModifiersSet;
  private final Type myType;
  private final Expression myInitializer;

  public LocalVariable(Identifier identifier, HashSet<String> modifiersSet, Type type, Expression initializer) {
    myIdentifier = identifier;
    myModifiersSet = modifiersSet;
    myType = type;
    myInitializer = initializer;
  }

  public boolean hasModifier(String modifier) {
    return myModifiersSet.contains(modifier);
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
