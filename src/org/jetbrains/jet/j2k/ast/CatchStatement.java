package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class CatchStatement extends Statement {
  private final Parameter myVariable;
  private final Block myBlock;

  public CatchStatement(Parameter variable, Block block) {
    myVariable = variable;
    myBlock = block;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "catch" + SPACE + "(" + myVariable.toKotlin() + ")" + SPACE + myBlock.toKotlin();
  }
}
