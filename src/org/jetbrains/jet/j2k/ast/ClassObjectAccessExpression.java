package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ClassObjectAccessExpression extends Expression {
  private Element myTypeElement;

  public ClassObjectAccessExpression(Element typeElement) {
    myTypeElement = typeElement;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "getJavaClass" + "<" + myTypeElement.toKotlin() + ">";
  }
}
