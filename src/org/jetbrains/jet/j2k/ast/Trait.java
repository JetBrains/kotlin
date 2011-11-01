package org.jetbrains.jet.j2k.ast;

import java.util.List;

/**
 * @author ignatov
 */
public class Trait extends Class {
  public Trait(Identifier name, List<Element> typeParameters, List<Type> extendsTypes, List<Type> implementsTypes, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    super(name, typeParameters, extendsTypes, implementsTypes, innerClasses, methods, fields);
    TYPE = "trait";
  }
}
