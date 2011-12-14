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
    if (myType.getKind() == Kind.ARRAY_TYPE) {
      if (myExpressions.size() == 1)
        return oneDim(myType, myExpressions.get(0));
      if (myExpressions.size() == 2) {
        Type innerType = ((ArrayType) myType).getInnerType();
        return oneDim(myType, myExpressions.get(0), "{" + SPACE +
          oneDim(innerType, myExpressions.get(1)) + SPACE + "}");
      }
    }
    return getConstructorName(myType);
  }

  private static String oneDim(Type type, Expression size) {
    return oneDim(type, size, EMPTY);
  }

  private static String oneDim(Type type, Expression size, String init) {
    String commaWithInit = init.isEmpty() ? EMPTY : COMMA_WITH_SPACE + init;
    return getConstructorName(type) + "(" + size.toKotlin() + commaWithInit + ")";
  }

  public static String getConstructorName(Type type) {
//    if (type instanceof ArrayType) {
//      String innerTypeStr = ((ArrayType) type).getInnerType().toKotlin().toLowerCase();
//      if (PRIMITIVE_TYPES.contains(innerTypeStr))
//        return innerTypeStr + "Array";
//    }
    return AstUtil.replaceLastQuest(type.toKotlin());
  }
}
