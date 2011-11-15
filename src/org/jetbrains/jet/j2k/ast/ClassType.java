package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.Collections;
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

  public ClassType(Identifier type) {
    myType = type;
    myNullable = false;
    myParameters = Collections.emptyList();
  }

  @NotNull
  @Override
  public String toKotlin() {
    String params = myParameters.size() == 0 ? EMPTY : "<" + AstUtil.joinNodes(myParameters, COMMA_WITH_SPACE) + ">";
    return myType.toKotlin() + params + isNullableStr();
  }
}
