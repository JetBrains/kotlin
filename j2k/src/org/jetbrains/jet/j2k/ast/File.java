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
  private final String myMainFunction;

  public File(String packageName, List<Import> imports, List<Class> classes, String mainFunction) {
    myPackageName = packageName;
    myImports = imports;
    myClasses = classes;
    myMainFunction = mainFunction;
  }

  @NotNull
  @Override
  public String toKotlin() {
    final String common = AstUtil.joinNodes(myImports, N) + N2 + AstUtil.joinNodes(myClasses, N) + N + myMainFunction;
    if (myPackageName.isEmpty())
      return common;
    return "package" + SPACE + myPackageName + N +
      common;
  }
}
