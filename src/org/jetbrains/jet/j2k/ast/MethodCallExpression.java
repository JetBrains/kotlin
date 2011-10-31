package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class MethodCallExpression extends Expression {
  private final Expression myMethodCall;
  private final Element myParamList;

  public MethodCallExpression(Expression methodCall, Element paramList) {
    myMethodCall = methodCall;
    myParamList = paramList;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myMethodCall.toKotlin() + "(" + myParamList.toKotlin() + ")";
  }
}
