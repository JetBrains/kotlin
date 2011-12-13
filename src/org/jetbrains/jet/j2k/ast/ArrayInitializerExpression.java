package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ArrayInitializerExpression extends Expression {
  private Type myType;
  private final List<Expression> myInitializers;

  public ArrayInitializerExpression(final Type type, List<Expression> initializers) {
    myType = type;
    myInitializers = initializers;

  }

  @NotNull
  private static String createArrayFunction(@NotNull final Type type) {
    String sType = type.convertedToNotNull().toKotlin().replace("Array", "").toLowerCase();
    if (!sType.equals("any") && PRIMITIVE_TYPES.contains(sType))
      return sType + "Array";
    return AstUtil.lowerFirstCharacter(type.convertedToNotNull().toKotlin());
  }

  @NotNull
  @Override
  public String toKotlin() {
    return createArrayFunction(myType) + "(" + AstUtil.joinNodes(myInitializers, COMMA_WITH_SPACE) + ")";
  }
}
