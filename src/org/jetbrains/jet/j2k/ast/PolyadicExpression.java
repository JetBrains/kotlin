package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class PolyadicExpression extends Expression {
  private List<Expression> myExpressions;
  private String myToken;

  public PolyadicExpression(List<Expression> expressions, String token) {
    super();
    myExpressions = expressions;
    myToken = token;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "(" + AstUtil.joinNodes(myExpressions, SPACE + myToken + SPACE) + ")";
  }
}
