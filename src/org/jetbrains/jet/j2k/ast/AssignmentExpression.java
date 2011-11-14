package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class AssignmentExpression extends Expression {
  private final Expression myLeft;
  private final Expression myRight;
  private final String myOp;

  public Expression getLeft() {
    return myLeft;
  }

  public Expression getRight() {
    return myRight;
  }

  public AssignmentExpression(Expression left, Expression right, String op) {
    myLeft = left;
    myRight = right;
    myOp = op;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myLeft.toKotlin() + SPACE + myOp + SPACE + myRight.toKotlin();
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.ASSINGNMENT_EXPRESSION;
  }
}
