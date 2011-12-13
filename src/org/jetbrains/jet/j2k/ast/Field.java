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
  @NotNull
  private final String myConversion;
  final Type myType;
  final Element myInitializer;

  public Field(Identifier identifier, Set<String> modifiers, Type type, Element initializer, @NotNull String conversionForCallChains) {
    myIdentifier = identifier;
    myConversion = conversionForCallChains;
    myModifiers = modifiers;
    myType = type;
    myInitializer = initializer;
  }

  public Field(Identifier identifier, Set<String> modifiers, Type type, Element initializer) {
    this(identifier, modifiers, type, initializer, "");
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

    modifierList.add(myModifiers.contains(Modifier.FINAL) ? "val" : "var");

    if (modifierList.size() > 0)
      return AstUtil.join(modifierList, SPACE) + SPACE;

    return EMPTY;
  }

  @Override
  public boolean isStatic() {
    return myModifiers.contains(Modifier.STATIC);
  }

  public boolean isFinal() {
    return myModifiers.contains(Modifier.FINAL);
  }

  @NotNull
  @Override
  public String toKotlin() {
    String modifier = modifiersToKotlin();

    if (myInitializer.isEmpty())
      return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE + EQUAL + SPACE + getDefaultInitializer(this);

    return modifier + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin() + SPACE +
      EQUAL + SPACE + myInitializer.toKotlin() + myConversion;
  }
}