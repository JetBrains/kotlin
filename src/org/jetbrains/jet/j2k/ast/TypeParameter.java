package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class TypeParameter extends Element {
  private Identifier myName;

  public TypeParameter(Identifier name) {
    myName = name;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myName.toKotlin();
  }
}