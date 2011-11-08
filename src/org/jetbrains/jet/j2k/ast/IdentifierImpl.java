package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class IdentifierImpl extends Expression implements Identifier {
  private final String myName;
  private boolean myHasDollar;
  private boolean myNullable = true;

  public IdentifierImpl(String name) {
    myName = name;
  }

  public IdentifierImpl(String name, boolean nullable) {
    myName = name;
    myNullable = nullable;
  }

  public IdentifierImpl(String name, boolean hasDollar, boolean nullable) {
    myName = name;
    myHasDollar = hasDollar;
    myNullable = nullable;
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

  @Override
  public boolean isNullable() {
    return myNullable;
  }

  private String ifNeedQuote(String name) {
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