package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class PolyadicExpression extends Expression {
  private final List<Expression> myExpressions;
  private final String myToken;
  private final List<String> myConversions;

  public PolyadicExpression(List<Expression> expressions, String token, List<String> conversions) {
    super();
    myExpressions = expressions;
    myToken = token;
    myConversions = conversions;
  }

  @NotNull
  @Override
  public String toKotlin() {
    List<String> expressionsWithConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(myExpressions), myConversions);
    return "(" + AstUtil.join(expressionsWithConversions, SPACE + myToken + SPACE) + ")";
  }
}
