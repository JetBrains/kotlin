package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.*;

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
    String sType = innerTypeStr(type);
    if (!sType.equals("any") && PRIMITIVE_TYPES.contains(sType))
      return sType + "Array";
    return AstUtil.lowerFirstCharacter(type.convertedToNotNull().toKotlin());
  }

  @NotNull
  private static String innerTypeStr(@NotNull final Type type) {
    return type.convertedToNotNull().toKotlin().replace("Array", "").toLowerCase();
  }

  @NotNull
  private static String createInitializers(@NotNull final Type type, @NotNull final List<Expression> initializers) {
    List<String> arguments = new LinkedList<String>();
    for (Expression i : initializers)
      arguments.add(explicitConvertIfNeeded(type, i));
    return AstUtil.join(arguments, COMMA_WITH_SPACE);
  }

  @NotNull
  private static String explicitConvertIfNeeded(@NotNull final Type type, @NotNull final Expression i) {
    Set<String> doubleOrFloatTypes = new HashSet<String>(
      Arrays.asList("double", "float", "java.lang.double", "java.lang.float")
    );
    String afterReplace = innerTypeStr(type).replace(">", "").replace("<", "").replace("?", "");
    if (doubleOrFloatTypes.contains(afterReplace)) {
      if (i.getKind() == Kind.LITERAL) {
        if (i.toKotlin().contains("."))
          return i.toKotlin();
        return i.toKotlin() + DOT + ZERO;
      }
      return "(" + i.toKotlin() + ")" + getConversion(afterReplace);
    }
    return i.toKotlin();
  }

  @NotNull
  private static String getConversion(@NotNull final String afterReplace) {
    if (afterReplace.contains("double")) return DOT + "dbl";
    if (afterReplace.contains("float")) return DOT + "flt";
    return "";
  }

  @NotNull
  @Override
  public String toKotlin() {
    return createArrayFunction(myType) + "(" + createInitializers(myType, myInitializers) + ")";
  }
}
