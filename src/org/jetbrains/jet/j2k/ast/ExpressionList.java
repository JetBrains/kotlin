package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ExpressionList extends Expression {
  private List<Expression> myExpressions;
  private List<Type> myTypes; // TODO: add types to toKotlin

  public ExpressionList(List<Expression> expressions, List<Type> types) {
    myExpressions = expressions;
    myTypes = types;
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
