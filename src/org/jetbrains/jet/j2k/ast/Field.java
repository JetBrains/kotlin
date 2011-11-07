package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Field extends Member {
  final Identifier myIdentifier;
  private final Type myType;
  final Element myInitializer;

  public Field(Identifier identifier, Set<String> modifiers, Type type, Element initializer) {
    myIdentifier = identifier;
    myModifiers = modifiers;
    myType = type;
    myInitializer = initializer;
  }

  String modifiersToKotlin() {
    List<String> modifierList = new LinkedList<String>();

    if (isAbstract())
      modifierList.add(Modifier.ABSTRACT);

    modifierList.add(accessModifier());

    modifierList.add(myModifiers.contains(Modifier.FINAL) ? "val" : "var");

    if (modifierList.size() > 0)
      return AstUtil.join(modifierList, SPACE) + SPACE;

    return EMPTY;
  }

  @Override
  public boolean isStatic() {
    return myModifiers.contains(Modifier.STATIC);
  }

  @NotNull
  @Override
  public String toKotlin() {
    String modifier = modifiersToKotlin();

    if (myInitializer.toKotlin().isEmpty()) // TODO: remove
      return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

    return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      EQUAL + SPACE + myInitializer.toKotlin();
  }
}