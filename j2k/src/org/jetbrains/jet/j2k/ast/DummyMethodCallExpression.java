package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DummyMethodCallExpression extends Expression {
  private final Element myWho;
  private final String myMethodName;
  private final Element myWhat;

  public DummyMethodCallExpression(Element who, String methodName, Element what) {
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
