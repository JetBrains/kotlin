package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class MethodCallExpression extends Expression {
  private final Expression myMethodCall;
  private final Element myParamList;
  private final boolean myIsResultNullable;

  public MethodCallExpression(Expression methodCall, Element paramList, boolean nullable) {
    myMethodCall = methodCall;
    myParamList = paramList;
    myIsResultNullable = nullable;
  }

  @Override
  public boolean isNullable() {
    return myIsResultNullable;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myMethodCall.toKotlin() + "(" + myParamList.toKotlin() + ")";
  }
}
