package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ExpressionListStatement extends Expression {
  private List<Expression> myExpressions;

  public ExpressionListStatement(List<Expression> expressions) {
    myExpressions = expressions;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return AstUtil.joinNodes(myExpressions, N);
  }
}
