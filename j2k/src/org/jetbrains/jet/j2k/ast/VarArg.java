package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class VarArg extends Type {
  private final Type myType;

  public VarArg(Type type) {
    myType = type;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.VARARG;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myType.toKotlin();
  }
}
