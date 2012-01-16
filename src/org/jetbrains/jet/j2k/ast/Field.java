package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.j2k.Converter.getDefaultInitializer;

/**
 * @author ignatov
 */
public class Field extends Member {
  final Identifier myIdentifier;
  private final int myWritingAccesses;
  final Type myType;
  final Element myInitializer;

  public Field(Identifier identifier, Set<String> modifiers, Type type, Element initializer, int writingAccesses) {
    myIdentifier = identifier;
    myWritingAccesses = writingAccesses;
    myModifiers = modifiers;
    myType = type;
    myInitializer = initializer;
  }

  public Element getInitializer() {
    return myInitializer;
  }

  public Identifier getIdentifier() {
    return myIdentifier;
  }

  public Type getType() {
    return myType;
  }

  @NotNull
  String modifiersToKotlin() {
    List<String> modifierList = new LinkedList<String>();

    if (isAbstract())
      modifierList.add(Modifier.ABSTRACT);

    modifierList.add(accessModifier());

    modifierList.add(isVal() ? "val" : "var");

    if (modifierList.size() > 0)
      return AstUtil.join(modifierList, SPACE) + SPACE;

    return EMPTY;
  }

  public boolean isVal() {
    return myModifiers.contains(Modifier.FINAL);
  }

  @Override
  public boolean isStatic() {
    return myModifiers.contains(Modifier.STATIC);
  }

  @NotNull
  @Override
  public String toKotlin() {
    final String declaration = modifiersToKotlin() + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

    if (myInitializer.isEmpty())
      return declaration + (isVal() && !isStatic() && myWritingAccesses == 1 ? EMPTY : SPACE + EQUAL + SPACE + getDefaultInitializer(this));

    return declaration + SPACE + EQUAL + SPACE + myInitializer.toKotlin();
  }
}