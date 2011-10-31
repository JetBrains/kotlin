package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class ForeachStatement extends Statement {
  private final Parameter myVariable;
  private final Expression myExpression;
  private final Statement myStatement;

  public ForeachStatement(Parameter variable, Expression expression, Statement statement) {
    myVariable = variable;
    myExpression = expression;
    myStatement = statement;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "for" + SPACE + "(" + myVariable.toKotlin() + SPACE + IN + SPACE + myExpression.toKotlin() + ")" + N +
      myStatement.toKotlin();
  }
}