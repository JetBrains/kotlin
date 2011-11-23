package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ForeachWithRangeStatement extends Statement {
  private Expression myStart;
  private IdentifierImpl myIdentifier;
  private Expression myEnd;
  private Statement myBody;

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
