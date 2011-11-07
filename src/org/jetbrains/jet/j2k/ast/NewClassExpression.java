package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ignatov
 */
public class NewClassExpression extends Expression {
  private final Element myName;
  private final Element myArguments;
  private AnonymousClass myAnonymousClass = null;

  public NewClassExpression(Element name, Element arguments) {
    myName = name;
    myArguments = arguments;
  }

  public NewClassExpression(Element name, Element arguments, @Nullable AnonymousClass anonymousClass) {
    this(name, arguments);
    myAnonymousClass = anonymousClass;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myAnonymousClass != null ?
      myName.toKotlin() + "(" + myArguments.toKotlin() + ")" + myAnonymousClass.toKotlin() :
      myName.toKotlin() + "(" + myArguments.toKotlin() + ")";
  }
}