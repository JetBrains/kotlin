package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class SwitchContainer extends Statement {
  private final Expression myExpression;
  private final List<CaseContainer> myCaseContainers;

  public SwitchContainer(final Expression expression, final List<CaseContainer> caseContainers) {
    myExpression = expression;
    myCaseContainers = caseContainers;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "when" + SPACE + "(" + myExpression.toKotlin() + ")" + SPACE + "{" + N +
      AstUtil.joinNodes(myCaseContainers, N) + N +
      "}";
  }
}
