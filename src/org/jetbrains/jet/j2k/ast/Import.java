package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class Import extends Node {
  private String myName;

  public Import(String name) {
    myName = name;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return "import" + SPACE + myName;
  }
}
