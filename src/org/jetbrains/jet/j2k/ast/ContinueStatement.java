package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ContinueStatement extends Statement {
  private Identifier myLabel = Identifier.EMPTY_IDENTIFIER;

  public ContinueStatement(Identifier label) {
    myLabel = label;
  }

  public ContinueStatement() {
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.CONTINUE;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myLabel.isEmpty())
      return "continue";
    return "continue" + AT + myLabel.toKotlin();
  }
}
