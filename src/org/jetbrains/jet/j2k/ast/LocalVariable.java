package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author ignatov
 */
public class LocalVariable extends Expression {
  private final Identifier myIdentifier;
  private final Set<String> myModifiersSet;
  private final Type myType;
  private final Expression myInitializer;
  private final String myConversion;

  public LocalVariable(Identifier identifier, Set<String> modifiersSet, Type type, Expression initializer, @NotNull String conversionForExpression) {
    myIdentifier = identifier;
    myModifiersSet = modifiersSet;
    myType = type;
    myInitializer = initializer;
    myConversion = conversionForExpression;
  }

  public boolean hasModifier(String modifier) {
    return myModifiersSet.contains(modifier);
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myInitializer.isEmpty())
      return myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

    return myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      EQUAL + SPACE + myInitializer.toKotlin() + myConversion;
  }
}
