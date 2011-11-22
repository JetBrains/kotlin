package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class MethodCallExpression extends Expression {
  private final Expression myMethodCall;
  private final Element myParamList;
  private final boolean myIsResultNullable;
  private final List<Type> myTypeParameters;

  public MethodCallExpression(Expression methodCall, Element paramList, boolean nullable, List<Type> typeParameters) {
    myMethodCall = methodCall;
    myParamList = paramList;
    myIsResultNullable = nullable;
    myTypeParameters = typeParameters;
  }

  @Override
  public boolean isNullable() {
    return myIsResultNullable;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String typeParamsToKotlin = myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
    return myMethodCall.toKotlin() + typeParamsToKotlin + "(" + myParamList.toKotlin() + ")";
  }
}
