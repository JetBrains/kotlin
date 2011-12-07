package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DefaultSwitchLabelStatement extends Statement {
  @NotNull
  @Override
  public String toKotlin() {
    return "else";
  }
}
