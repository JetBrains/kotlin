package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class Class extends Node {
  protected String TYPE = "class";
  protected Identifier myName;
  protected List<Class> myInnerClasses;
  protected List<Function> myMethods;
  protected List<Field> myFields;

  public Class(Identifier name, List<Class> innerClasses, List<Function> methods, List<Field> fields) {
    myName = name;
    myInnerClasses = innerClasses;
    myMethods = methods;
    myFields = fields;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return TYPE + SPACE + myName.toKotlin() + SPACE + "{" + N +
      AstUtil.joinNodes(myInnerClasses, N) + N +
      AstUtil.joinNodes(myMethods, N) + N +
      AstUtil.joinNodes(myFields, N) + N +
      "}";
  }
}
