package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Enum extends Class {
  public Enum(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes, List<Expression> baseClassParams, List<Type> implementsTypes, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    super(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, innerClasses, methods, fields);
  }

  @Nullable
  private Constructor getPrimaryConstructor() {
    for (Function m : myMethods)
      if (m.getKind() == Kind.CONSTRUCTOR)
        return (Constructor) m;
    return null;
  }

  private String primaryConstructorToKotlin() {
    Constructor maybeConstructor = getPrimaryConstructor();
    if (maybeConstructor != null)
      return maybeConstructor.privatePrimaryToKotlin();
    return EMPTY;
  }

  @Override
  boolean needOpenModifier() {
    return false;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return modifiersToKotlin() + "enum" + SPACE + myName.toKotlin() + typeParametersToKotlin() + implementTypesToKotlin() + SPACE + "{" + N +
      AstUtil.joinNodes(myFields, N) + N +
      primaryConstructorToKotlin() + N +
      AstUtil.joinNodes(methodsExceptConstructors(), N) + N +
      AstUtil.joinNodes(myInnerClasses, N) + N +
      "}";
  }
}