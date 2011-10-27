package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Function extends Node {
  private Identifier myName;
  private Type myType;
  protected Element myParams;
  private Block myBlock;

  public Function(Identifier name, Type type, Element params, Block block) {
    myName = name;
    myType = type;
    myParams = params;
    myBlock = block;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "fun" + SPACE + myName.toKotlin() + "(" + myParams.toKotlin() + ")" + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      myBlock.toKotlin();
  }
}
