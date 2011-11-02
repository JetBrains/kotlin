package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ExpressionList extends Expression {
  private final List<Expression> myExpressions;

  public ExpressionList(List<Expression> expressions, List<Type> types) {
    myExpressions = expressions;
    List<Type> types1 = types;
  }

  public ExpressionList(List<Expression> expressions) {
    myExpressions = expressions;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return AstUtil.joinNodes(myExpressions, COMMA_WITH_SPACE);
  }
}
