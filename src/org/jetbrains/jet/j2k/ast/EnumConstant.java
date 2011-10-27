package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class EnumConstant extends Field {
  public EnumConstant(Identifier identifier, Type type, Element params) {
    super(identifier, type, params);
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myInitializer.toKotlin().isEmpty())
      return myIdentifier.toKotlin();
    return myIdentifier.toKotlin() + "(" + myInitializer.toKotlin() + ")";
  }
}
