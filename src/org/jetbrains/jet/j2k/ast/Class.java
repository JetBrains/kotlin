package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Class extends Node {
  String TYPE = "class";
  final Identifier myName;
  private Set<String> myModifiers;
  private final List<Element> myTypeParameters;
  private final List<Type> myExtendsTypes;
  private final List<Type> myImplementsTypes;
  final List<Class> myInnerClasses;
  final List<Function> myMethods;
  final List<Field> myFields;

  public Class(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
               List<Type> implementsTypes, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    myName = name;
    myModifiers = modifiers;
    myTypeParameters = typeParameters;
    myExtendsTypes = extendsTypes;
    myImplementsTypes = implementsTypes;
    myInnerClasses = innerClasses;
    myMethods = methods;
    myFields = fields;
  }

  private boolean hasWhere() {
    for (Element t : myTypeParameters)
      if (t instanceof TypeParameter && ((TypeParameter) t).hasWhere())
        return true;
    return false;
  }

  String typeParameterWhereToKotlin() {
    if (hasWhere()) {
      List<String> wheres = new LinkedList<String>();
      for (Element t : myTypeParameters)
        if (t instanceof TypeParameter)
          wheres.add(((TypeParameter) t).getWhereToKotlin());
      return SPACE + "where" + SPACE + AstUtil.join(wheres, COMMA_WITH_SPACE) + SPACE;
    }
    return EMPTY;
  }

  @NotNull
  List<Function> methodsExceptConstructors() {
    final LinkedList<Function> result = new LinkedList<Function>();
    for (Function m : myMethods)
      if (m.getKind() != Kind.CONSTRUCTOR)
        result.add(m);
    return result;
  }

  String typeParametersToKotlin() {
    return myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
  }

  String implementTypesToKotlin() {
    List<Type> allTypes = new LinkedList<Type>() {
      {
        addAll(myExtendsTypes);
        addAll(myImplementsTypes);
      }
    };
    return allTypes.size() == 0 ? EMPTY : SPACE + COLON + SPACE + AstUtil.joinNodes(allTypes, COMMA_WITH_SPACE);
  }

  private String accessModifier() {
    for (String m : myModifiers)
      if (m.equals(Modifier.PUBLIC) || m.equals(Modifier.PROTECTED) || m.equals(Modifier.PRIVATE))
        return m;
    return EMPTY; // package local converted to internal, but we use internal by default
  }

  String modifiersToKotlin() {
    List<String> modifierList = new LinkedList<String>();

    modifierList.add(accessModifier());

    if (needOpenModifier())
      modifierList.add(Modifier.OPEN);

    if (modifierList.size() > 0)
      return AstUtil.join(modifierList, SPACE) + SPACE;

    return EMPTY;
  }

  boolean needOpenModifier() {
    return getKind() != Kind.TRAIT && !myModifiers.contains(Modifier.FINAL);
  }


  @NotNull
  @Override
  public String toKotlin() {
    return modifiersToKotlin() + TYPE + SPACE + myName.toKotlin() + typeParametersToKotlin() +
      implementTypesToKotlin() +
      typeParameterWhereToKotlin() +
      SPACE + "{" + N +
      AstUtil.joinNodes(myFields, N) + N +
      AstUtil.joinNodes(methodsExceptConstructors(), N) + N +
      AstUtil.joinNodes(myInnerClasses, N) + N +
      "}";
  }
}
