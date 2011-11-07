package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class AnonymousClass extends Class {
  public AnonymousClass(List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    super(new IdentifierImpl("anonClass"),
      new HashSet<String>(),
      new LinkedList<Element>(),
      new LinkedList<Type>(),
      new LinkedList<Type>(),
      innerClasses,
      methods,
      fields);
  }

  @NotNull
  @Override
  public String toKotlin() {
    return bodyToKotlin();
  }
}