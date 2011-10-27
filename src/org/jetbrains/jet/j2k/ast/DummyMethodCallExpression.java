package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DummyMethodCallExpression extends Expression {
  private Expression myWho;
  private String myMethodName;
  private Expression myWhat;

  public DummyMethodCallExpression(Expression who, String methodName, Expression what) {
    myWho = who;
    myMethodName = methodName;
    myWhat = what;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myWho.toKotlin() + DOT + myMethodName + "(" + myWhat.toKotlin() + ")";
  }
}
