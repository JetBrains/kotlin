package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class NewClassExpression extends Expression {
  private final Element myName;
  private final List<Expression> myArguments;
  private Expression myQualifier;
  private List<String> myConversions;
  private AnonymousClass myAnonymousClass = null;

  public NewClassExpression(Element name, List<Expression> arguments) {
    myName = name;
    myQualifier = EMPTY_EXPRESSION;
    myArguments = arguments;
    myConversions = AstUtil.createListWithEmptyString(arguments);
  }

  public NewClassExpression(@NotNull Expression qualifier, @NotNull Element name, @NotNull List<Expression> arguments,
                            @NotNull List<String> conversions, @Nullable AnonymousClass anonymousClass) {
    this(name, arguments);
    myQualifier = qualifier;
    myConversions = conversions;
    myAnonymousClass = anonymousClass;
  }

  @NotNull
  @Override
  public String toKotlin() {
    final String callOperator = myQualifier.isNullable() ? QUESTDOT : DOT;
    final String qualifier = myQualifier.isEmpty() ? EMPTY : myQualifier.toKotlin() + callOperator;
    List<String> applyConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(myArguments), myConversions);
    String appliedArguments = AstUtil.join(applyConversions, COMMA_WITH_SPACE);
    return myAnonymousClass != null ?
      "object" + SPACE + ":" + SPACE + qualifier + myName.toKotlin() + "(" + appliedArguments + ")" + myAnonymousClass.toKotlin() :
      qualifier + myName.toKotlin() + "(" + appliedArguments + ")";
  }
}