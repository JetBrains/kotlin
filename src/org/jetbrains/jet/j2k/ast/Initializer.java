package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author ignatov
 */
public class Initializer extends Member {
  private Block myBlock;

  public Initializer(Block block, Set<String> modifiers) {
    myBlock = block;
    myModifiers = modifiers;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myBlock.toKotlin();
  }
}
