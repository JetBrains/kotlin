package org.jetbrains.jet.j2k.ast;

import java.util.List;

/**
 * @author ignatov
 */
public class Enum extends Class {
  public Enum(Identifier name, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    super(name, innerClasses, methods, fields);
    TYPE = "enum";
  }
}
