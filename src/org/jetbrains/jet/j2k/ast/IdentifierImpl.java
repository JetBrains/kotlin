package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class IdentifierImpl extends Expression implements Identifier {
  private final String myName;
  private boolean myHasDollar = false;
  private boolean myIsNullable = true;
  private boolean myQuotingNeeded = true;

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

  public IdentifierImpl(String name, boolean hasDollar, boolean isNullable, boolean quotingNeeded) {
    this(name, hasDollar,  isNullable);
    myQuotingNeeded = quotingNeeded;
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

  private String ifNeedQuote() {
    if (myQuotingNeeded && (ONLY_KOTLIN_KEYWORDS.contains(myName) || myName.contains("$")))
      return quote(myName);
    return myName;
  }

  @NotNull
  @Override
  public String toKotlin() {
    if (myHasDollar)
      return DOLLAR + ifNeedQuote();
    return ifNeedQuote();
  }
}