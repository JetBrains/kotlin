package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ForeachWithRangeStatement extends Statement {
  private final Expression myStart;
  private final IdentifierImpl myIdentifier;
  private final Expression myEnd;
  private final Statement myBody;

  public ForeachWithRangeStatement(IdentifierImpl identifier, Expression start, Expression end, Statement body) {
    myStart = start;
    myIdentifier = identifier;
    myEnd = end;
    myBody = body;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "for" + SPACE + "(" +
      myIdentifier.toKotlin() + SPACE + "in" + SPACE + myStart.toKotlin() + ".." + myEnd.toKotlin() +
      ")" + SPACE +
      myBody.toKotlin();
  }
}
