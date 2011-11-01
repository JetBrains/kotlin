package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class Enum extends Class {
  public Enum(Identifier name, List<Element> typeParameters, List<Type> extendsTypes, List<Type> implementsTypes, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    super(name, typeParameters, extendsTypes, implementsTypes, innerClasses, methods, fields);
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
      return maybeConstructor.primary();
    return EMPTY;
  }


  @NotNull
  @Override
  public String toKotlin() {
    return "enum" + SPACE + myName.toKotlin() + typeParametersToKotlin() + primaryConstructorToKotlin() + implementTypesToKotlin() + SPACE + "{" + N +
      AstUtil.joinNodes(myFields, N) + N +
      AstUtil.joinNodes(methodsExceptConstructors(), N) + N +
      AstUtil.joinNodes(myInnerClasses, N) + N +
      "}";
  }
}
