package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class IdentifierImpl extends Expression implements Identifier {
  private final String myName;
  private boolean myIsNullable = true;
  private boolean myQuotingNeeded = true;

  public IdentifierImpl(String name) {
    myName = name;
  }

  public IdentifierImpl(String name, boolean isNullable) {
    myName = name;
    myIsNullable = isNullable;
  }

  public IdentifierImpl(String name, boolean isNullable, boolean quotingNeeded) {
    myName = name;
    myIsNullable = isNullable;
    myQuotingNeeded = quotingNeeded;
  }

  @Override
  public boolean isEmpty() {
    return myName.length() == 0;
  }

  @Override
  public String getName() {
    return myName;
  }

  @NotNull
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
    return ifNeedQuote();
  }
}