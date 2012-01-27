package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class TryStatement extends Statement {
  private final Block myBlock;
  private final List<CatchStatement> myCatches;
  private final Block myFinallyBlock;

  public TryStatement(Block block, List<CatchStatement> catches, Block finallyBlock) {
    myBlock = block;
    myCatches = catches;
    myFinallyBlock = finallyBlock;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "try" + N +
      myBlock.toKotlin() + N +
      AstUtil.joinNodes(myCatches, N) + N +
      (myFinallyBlock.isEmpty() ? EMPTY : "finally" + N + myFinallyBlock.toKotlin());
  }
}