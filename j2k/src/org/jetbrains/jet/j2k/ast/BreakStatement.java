package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class BreakStatement extends Statement {
  private Identifier myLabel = Identifier.EMPTY_IDENTIFIER;

  public BreakStatement(Identifier label) {
    myLabel = label;
  }

  public BreakStatement() {
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.BREAK;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myLabel.isEmpty())
      return "break";
    return "break" + AT + myLabel.toKotlin();
  }
}
