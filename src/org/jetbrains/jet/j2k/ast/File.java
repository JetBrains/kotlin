package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class File extends Node {
  private final String myPackageName;
  private final List<Import> myImports;
  private final List<Class> myClasses;

  public File(String packageName, List<Import> imports, List<Class> classes) {
    myPackageName = packageName;
    myImports = imports;
    myClasses = classes;
  }

  @NotNull
  @Override
  public String toKotlin() {
    final String common = AstUtil.joinNodes(myImports, N) + N2 + AstUtil.joinNodes(myClasses, N) + N;
    if (myPackageName.isEmpty())
      return common;
    return "namespace" + SPACE + myPackageName + SPACE + "{" + N +
      common +
      "}";
  }
}
