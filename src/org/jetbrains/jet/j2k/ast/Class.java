package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class Class extends Node {
  String TYPE = "class";
  final Identifier myName;
  final List<Class> myInnerClasses;
  final List<Function> myMethods;
  final List<Field> myFields;

  public Class(Identifier name, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    myName = name;
    myInnerClasses = innerClasses;
    myMethods = methods;
    myFields = fields;
  }

  @NotNull
  List<Function> methodsExceptConstructors() {
    final LinkedList<Function> result = new LinkedList<Function>();
    for (Function m : myMethods)
      if (m.getKind() != Kind.CONSTRUCTOR)
        result.add(m);
    return result;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return TYPE + SPACE + myName.toKotlin() + SPACE + "{" + N +
      AstUtil.joinNodes(myFields, N) + N +
      AstUtil.joinNodes(methodsExceptConstructors(), N) + N +
      AstUtil.joinNodes(myInnerClasses, N) + N +
      "}";
  }
}
