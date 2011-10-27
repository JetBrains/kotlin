package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Function extends Node {
  private Identifier myName;
  private Type myType;
  private Block myBlock;

  public Function(Identifier name, Type type, Block block) {
    myName = name;
    myType = type;
    myBlock = block;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "fun" + SPACE + myName.toKotlin() + "(" + ")" + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      myBlock.toKotlin();
  }
}
