package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * @author ignatov
 */
public class EnumConstant extends Field {
  public EnumConstant(Identifier identifier, HashSet<String> modifiers, Type type, Element params) {
    super(identifier, modifiers, type, params);
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myInitializer.toKotlin().isEmpty())
      return myIdentifier.toKotlin();
    return myIdentifier.toKotlin() + "(" + myInitializer.toKotlin() + ")";
  }
}
