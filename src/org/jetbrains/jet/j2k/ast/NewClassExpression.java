package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class NewClassExpression extends Expression {
  private final Element myName;
  private final Element myArguments;

  public NewClassExpression(Element name, Element arguments) {
    myName = name;
    myArguments = arguments;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myName.toKotlin() + "(" + myArguments.toKotlin() + ")";
  }
}