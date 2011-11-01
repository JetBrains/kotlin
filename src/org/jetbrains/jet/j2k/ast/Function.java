package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class Function extends Node {
  private final Identifier myName;
  private final Type myType;
  private List<Element> myTypeParameters;
  final Element myParams;
  private final Block myBlock;

  public Function(Identifier name, Type type, List<Element> typeParameters, Element params, Block block) {
    myName = name;
    myType = type;
    myTypeParameters = typeParameters;
    myParams = params;
    myBlock = block;
  }

  private String typeParametersToKotlin() {
    return myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "fun" + SPACE + myName.toKotlin() + typeParametersToKotlin() + "(" + myParams.toKotlin() + ")" + SPACE + COLON +
      SPACE + myType.toKotlin() + SPACE + myBlock.toKotlin();
  }
}
