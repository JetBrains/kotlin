package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class IdentifierImpl extends Expression implements Identifier {
  private final String myName;

  public IdentifierImpl(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public boolean isEmpty() {
    return myName.length() == 0;
  }

  private String quote(String str) {
    return BACKTICK + str + BACKTICK;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (ONLY_KOTLIN_KEYWORDS.contains(myName))
      return quote(myName);
    return myName;
  }
}