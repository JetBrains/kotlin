package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ParameterList extends Expression {
  private final List<Parameter> myParameters;

  public ParameterList(List<Parameter> parameters) {
    myParameters = parameters;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return AstUtil.joinNodes(myParameters, COMMA_WITH_SPACE);
  }
}
