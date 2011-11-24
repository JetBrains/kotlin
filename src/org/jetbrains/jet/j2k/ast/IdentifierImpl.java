package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class IdentifierImpl extends Expression implements Identifier {
  private final String myName;
  private boolean myHasDollar = false;
  private boolean myIsNullable = true;

  public IdentifierImpl(String name) {
    myName = name;
  }

  public IdentifierImpl(String name, boolean isNullable) {
    myName = name;
    myIsNullable = isNullable;
  }

  public IdentifierImpl(String name, boolean hasDollar, boolean isNullable) {
    myName = name;
    myHasDollar = hasDollar;
    myIsNullable = isNullable;
  }

  @Override
  public boolean isEmpty() {
    return myName.length() == 0;
  }

  private static String quote(String str) {
    return BACKTICK + str + BACKTICK;
  }

  @Override
  public boolean isNullable() {
    return myIsNullable;
  }

  private static String ifNeedQuote(String name) {
    if (ONLY_KOTLIN_KEYWORDS.contains(name) || name.contains("$"))
      return quote(name);
    return name;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myHasDollar)
      return DOLLAR + ifNeedQuote(myName);
    return ifNeedQuote(myName);
  }
}