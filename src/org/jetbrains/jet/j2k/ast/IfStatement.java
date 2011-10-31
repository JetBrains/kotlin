package org.jetbrains.jet.j2k.ast;

/**
 * @author ignatov
 */
public class IfStatement extends Statement {
  private final Expression myCondition;
  private final Statement myThenStatement;
  private final Statement myElseStatement;

  public IfStatement(Expression condition, Statement thenStatement, Statement elseStatement) {
    myCondition = condition;
    myThenStatement = thenStatement;
    myElseStatement = elseStatement;
  }

  @Override
  public String toKotlin() {
    String result = "if" + SPACE + "(" + myCondition.toKotlin() + ")" + N + myThenStatement.toKotlin() + N;

    if (myElseStatement != Statement.EMPTY_STATEMENT)
      return result +
        "else" + N +
        myElseStatement.toKotlin();

    return result;
  }
}