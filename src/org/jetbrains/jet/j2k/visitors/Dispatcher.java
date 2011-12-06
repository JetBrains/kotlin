package org.jetbrains.jet.j2k.visitors;

/**
 * @author ignatov
 */
public class Dispatcher {
  private ExpressionVisitor myExpressionVisitor;

  public void setExpressionVisitor(final ExpressionVisitor expressionVisitor) {
    this.myExpressionVisitor = expressionVisitor;
  }

  public Dispatcher() {
    myExpressionVisitor = new ExpressionVisitor();
  }

  public ExpressionVisitor getExpressionVisitor() {
    return myExpressionVisitor;
  }
}
