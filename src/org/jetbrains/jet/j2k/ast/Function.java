package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Function extends Node {
  private final Identifier myName;
  private Set<String> myModifiers;
  private final Type myType;
  private final List<Element> myTypeParameters;
  final Element myParams;
  private final Block myBlock;

  public Function(Identifier name, Set<String> modifiers, Type type, List<Element> typeParameters, Element params, Block block) {
    myName = name;
    myModifiers = modifiers;
    myType = type;
    myTypeParameters = typeParameters;
    myParams = params;
    myBlock = block;
  }

  private String typeParametersToKotlin() {
    return myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
  }

  private boolean hasWhere() {
    for (Element t : myTypeParameters)
      if (t instanceof TypeParameter && ((TypeParameter) t).hasWhere())
        return true;
    return false;
  }

  private String accessModifier() {
    for (String m : myModifiers)
      if (m.equals(Modifier.PUBLIC) || m.equals(Modifier.PROTECTED) || m.equals(Modifier.PRIVATE))
        return m;
    return EMPTY; // package local converted to internal, but we use internal by default
  }

  private String typeParameterWhereToKotlin() {
    if (hasWhere()) {
      List<String> wheres = new LinkedList<String>();
      for (Element t : myTypeParameters)
        if (t instanceof TypeParameter)
          wheres.add(((TypeParameter) t).getWhereToKotlin());
      return SPACE + "where" + SPACE + AstUtil.join(wheres, COMMA_WITH_SPACE) + SPACE;
    }
    return EMPTY;
  }

  private String modifiersToKotlin() {
    List<String> modifierList = new LinkedList<String>();

    modifierList.add(accessModifier());

    if (modifierList.size() > 0)
      return AstUtil.join(modifierList, SPACE) + SPACE;

    return EMPTY;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return modifiersToKotlin() + "fun" + SPACE + myName.toKotlin() + typeParametersToKotlin() + "(" + myParams.toKotlin() + ")" + SPACE + COLON +
      SPACE + myType.toKotlin() + SPACE +
      typeParameterWhereToKotlin() +
      myBlock.toKotlin();
  }
}
