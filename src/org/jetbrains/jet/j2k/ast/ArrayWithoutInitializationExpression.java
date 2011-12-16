package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ArrayWithoutInitializationExpression extends Expression {
  private final Type myType;
  private final List<Expression> myExpressions;

  public ArrayWithoutInitializationExpression(Type type, List<Expression> expressions) {
    myType = type;
    myExpressions = expressions;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myType.getKind() == Kind.ARRAY_TYPE)
      return constructInnerType((ArrayType) myType, myExpressions);
    return getConstructorName(myType);
  }

  @NotNull
  private static String constructInnerType(@NotNull ArrayType hostType, @NotNull List<Expression> expressions) {
    if (expressions.size() == 1)
      return oneDim(hostType, expressions.get(0));
    Type innerType = hostType.getInnerType();
    if (expressions.size() > 1 && innerType.getKind() == Kind.ARRAY_TYPE)
      return oneDim(hostType, expressions.get(0), "{" + constructInnerType((ArrayType) innerType, expressions.subList(1, expressions.size())) + "}");
    return getConstructorName(hostType);
  }

  @NotNull
  private static String oneDim(@NotNull Type type, @NotNull Expression size) {
    return oneDim(type, size, EMPTY);
  }

  @NotNull
  private static String oneDim(@NotNull Type type, @NotNull Expression size, @NotNull String init) {
    String commaWithInit = init.isEmpty() ? EMPTY : COMMA_WITH_SPACE + init;
    return getConstructorName(type) + "(" + size.toKotlin() + commaWithInit + ")";
  }

  @NotNull
  private static String getConstructorName(@NotNull Type type) {
    return AstUtil.replaceLastQuest(type.toKotlin());
  }
}
