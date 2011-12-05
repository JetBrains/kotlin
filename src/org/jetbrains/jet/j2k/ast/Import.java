package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Import extends Node {
  private final String myName;

  public String getName() {
    return myName;
  }

  public Import(String name) {
    myName = name;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "import" + SPACE + myName;
  }
}
