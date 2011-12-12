package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class MethodCallExpression extends Expression {
  private final Expression myMethodCall;
  private final List<Expression> myArguments;
  private List<String> myConversions;
  private final boolean myIsResultNullable;
  private final List<Type> myTypeParameters;

  public MethodCallExpression(Expression methodCall, List<Expression> arguments, boolean nullable, List<Type> typeParameters) {
    this(methodCall, arguments, AstUtil.createListWithEmptyString(arguments), nullable, typeParameters);
  }

  public MethodCallExpression(Expression methodCall, List<Expression> arguments, List<String> conversions, boolean nullable, List<Type> typeParameters) {
    myMethodCall = methodCall;
    myArguments = arguments;
    myConversions = conversions;
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
    List<String> applyConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(myArguments), myConversions);
    return myMethodCall.toKotlin() + typeParamsToKotlin + "(" + AstUtil.join(applyConversions, COMMA_WITH_SPACE) + ")";
  }
}
