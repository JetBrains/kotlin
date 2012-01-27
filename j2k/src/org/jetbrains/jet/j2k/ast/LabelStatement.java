package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class LabelStatement extends Statement {
  private final Identifier myName;
  private final Statement myStatement;

  public LabelStatement(Identifier name, Statement statement) {
    myName = name;
    myStatement = statement;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return AT + myName.toKotlin() + SPACE + myStatement.toKotlin();
  }
}
