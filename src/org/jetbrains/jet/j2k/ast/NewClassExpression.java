package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ignatov
 */
public class NewClassExpression extends Expression {
  private final Element myName;
  private final Element myArguments;
  private Expression myQualifier;
  private AnonymousClass myAnonymousClass = null;

  public NewClassExpression(Element name, Element arguments) {
    myName = name;
    myQualifier = EMPTY_EXPRESSION;
    myArguments = arguments;
  }

  public NewClassExpression(Expression qualifier, Element name, Element arguments, @Nullable AnonymousClass anonymousClass) {
    this(name, arguments);
    myQualifier = qualifier;
    myAnonymousClass = anonymousClass;
  }

  @NotNull
  @Override
  public String toKotlin() {
    final String callOperator = myQualifier.isNullable() ? QUESTDOT : DOT;
    final String qualifier = myQualifier.isEmpty() ? EMPTY : myQualifier.toKotlin() + callOperator;
    return myAnonymousClass != null ?
      qualifier + myName.toKotlin() + "(" + myArguments.toKotlin() + ")" + myAnonymousClass.toKotlin() :
      qualifier + myName.toKotlin() + "(" + myArguments.toKotlin() + ")";
  }
}