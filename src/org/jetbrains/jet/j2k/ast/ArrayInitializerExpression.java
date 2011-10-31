package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ArrayInitializerExpression extends Expression {
  private final List<Expression> myInitializers;

  public ArrayInitializerExpression(List<Expression> initializers) {
    myInitializers = initializers;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "array" + "(" + AstUtil.joinNodes(myInitializers, COMMA_WITH_SPACE) + ")";
  }
}
