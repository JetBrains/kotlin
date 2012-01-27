package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class CallChainExpression extends Expression {
  private final Expression myExpression;
  private final Expression myIdentifier;

  public Expression getIdentifier() {
    return myIdentifier;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.CALL_CHAIN;
  }

  public CallChainExpression(Expression expression, Expression identifier) {
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
