package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author ignatov
 */
public class BinaryExpression extends Expression {
  private final Expression myLeft;
  private final Expression myRight;
  private final String myOp;
  private final List<String> myConversions;

  public BinaryExpression(Expression left, Expression right, String op) {
    this(left, right, op, Arrays.asList("", ""));
  }

  public BinaryExpression(Expression left, Expression right, String op, List<String> conversions) {
    myLeft = left;
    myRight = right;
    myOp = op;
    myConversions = conversions;
  }

  @NotNull
  @Override
  public String toKotlin() {
    List<String> expressionsWithConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(Arrays.asList(myLeft, myRight)), myConversions);
    return "(" + AstUtil.join(expressionsWithConversions, SPACE + myOp + SPACE) + ")";
  }
}
