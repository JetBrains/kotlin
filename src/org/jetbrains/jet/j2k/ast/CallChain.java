package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class CallChain extends Expression {
  private Expression myExpression;
  private IdentifierImpl myIdentifier;

  public CallChain(Expression expression, IdentifierImpl identifier) {
    myExpression = expression;
    myIdentifier = identifier;
  }

  @Override
  public boolean isNullable() {
    return myIdentifier.isNullable();
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (!myExpression.isEmpty())
      if (myExpression.isNullable())
        return myExpression.toKotlin() + QUESTDOT + myIdentifier.toKotlin();
      else
        return myExpression.toKotlin() + DOT + myIdentifier.toKotlin();
    return myIdentifier.toKotlin();
  }
}
