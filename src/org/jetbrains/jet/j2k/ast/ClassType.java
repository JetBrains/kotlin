package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class ClassType extends Type {
  private final Identifier myType;
  private final List<Type> myParameters;

  public ClassType(Identifier type, List<Type> parameters) {
    myType = type;
    myParameters = parameters;
  }

  public ClassType(Identifier type, boolean isNullable) {
    myType = type;
    myNullable = isNullable;
    myParameters = new LinkedList<Type>();
  }

  @NotNull
  @Override
  public String toKotlin() {
    String params = myParameters.size() == 0 ? EMPTY : "<" + AstUtil.joinNodes(myParameters, COMMA_WITH_SPACE) + ">";
    return myType.toKotlin() + params + isNullableStr();
  }
}
