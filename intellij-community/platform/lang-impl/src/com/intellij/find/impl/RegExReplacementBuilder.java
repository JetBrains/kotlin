/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.find.impl;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * Generates a replacement string for search/replace operation using regular expressions.
 * <p>
 * The logic is based on java.util.regex.Matcher.appendReplacement method, special characters (\n, \r, \t, \f, \b, \xNNNN)
 * and case conversion characters (\l, &#92;u, \L, \U, \E) are additionally supported.
 * <p>
 * Instances of this class are not safe for use by multiple concurrent threads, just as {@link Matcher} instances are.
 */
public class RegExReplacementBuilder {
  @NotNull private final Matcher myMatcher;

  private String myTemplate;
  private int myCursor;
  private StringBuilder myReplacement;
  private List<CaseConversionRegion> myConversionRegions;

  public RegExReplacementBuilder(@NotNull Matcher matcher) {
    myMatcher = matcher;
  }

  /**
   * Generates a replacement string from provided template value, substituting referenced capturing group values, and processing supported
   * special and control characters.
   * <p>
   * Matcher used to create this instance of RegExReplacementBuilder is supposed to be in a state
   * created by a successful {@link Matcher#find() find()} or {@link Matcher#find(int) find(int)} invocation.
   */
  public String createReplacement(String template) {
    myTemplate = template;
    resetState();
    while (myCursor < myTemplate.length()) {
      char nextChar = myTemplate.charAt(myCursor++);
      if (nextChar == '\\') {
        processEscapedChar();
      } else if (nextChar == '$') {
        processGroupValue();
      } else {
        myReplacement.append(nextChar);
      }
    }
    return generateResult();
  }

  private void resetState() {
    myCursor = 0;
    myReplacement = new StringBuilder();
    myConversionRegions = new ArrayList<>();
  }

  private void processEscapedChar() {
    char nextChar;
    if (myCursor == myTemplate.length()) throw new IllegalArgumentException("character to be escaped is missing");
    nextChar = myTemplate.charAt(myCursor++);
    switch (nextChar) {
      case 'n':
        myReplacement.append('\n'); break;
      case 'r':
        myReplacement.append('\r'); break;
      case 'b':
        myReplacement.append('\b');  break;
      case 't':
        myReplacement.append('\t'); break;
      case 'f':
        myReplacement.append('\f'); break;
      case 'x':
        if (myCursor + 4 <= myTemplate.length()) {
          try {
            int code = Integer.parseInt(myTemplate.substring(myCursor, myCursor + 4), 16);
            myCursor += 4;
            myReplacement.append((char)code);
          }
          catch (NumberFormatException ignored) {}
        }
        break;
      case 'l': startConversionForCharacter(false); break;
      case 'u': startConversionForCharacter(true); break;
      case 'L': startConversionForRegion(false); break;
      case 'U': startConversionForRegion(true); break;
      case 'E': resetConversionState(); break;
      default:
        myReplacement.append(nextChar);
    }
  }

  private void processGroupValue() {
    char nextChar;
    if (myCursor == myTemplate.length()) throw new IllegalArgumentException("Illegal group reference: group index is missing");
    nextChar = myTemplate.charAt(myCursor++);
    String group;
    if (nextChar == '{') {
      StringBuilder gsb = new StringBuilder();
      while (myCursor < myTemplate.length()) {
        nextChar = myTemplate.charAt(myCursor);
        if (isLatinLetter(nextChar) || isDigit(nextChar)) {
          gsb.append(nextChar);
          myCursor++;
        } else {
          break;
        }
      }
      if (gsb.length() == 0) throw new IllegalArgumentException("named capturing group has 0 length name");
      if (nextChar != '}') throw new IllegalArgumentException("named capturing group is missing trailing '}'");
      String gname = gsb.toString();
      if (isDigit(gname.charAt(0))) {
        throw new IllegalArgumentException("capturing group name {" + gname + "} starts with digit character");
      }
      myCursor++;
      group = myMatcher.group(gname);
    } else {
      // The first number is always a group
      int refNum = (int)nextChar - '0';
      if (refNum < 0 || refNum > 9) throw new IllegalArgumentException("Illegal group reference");
      // Capture the largest legal group string
      while (true) {
        if (myCursor >= myTemplate.length()) break;
        int nextDigit = myTemplate.charAt(myCursor) - '0';
        if (nextDigit < 0 || nextDigit > 9) break;
        int newRefNum = (refNum * 10) + nextDigit;
        if (myMatcher.groupCount() < newRefNum) break;
        refNum = newRefNum;
        myCursor++;
      }
      group = myMatcher.group(refNum);
    }
    if (group != null) {
      myReplacement.append(group);
    }
  }

  private String generateResult() {
    StringBuilder result;
    if (myConversionRegions.isEmpty()) {
      result = myReplacement;
    }
    else {
      CaseConversionRegion lastRegion = myConversionRegions.get(myConversionRegions.size() - 1);
      if (lastRegion.end < 0 || lastRegion.end > myReplacement.length()) {
        lastRegion.end = myReplacement.length();
      }
      result = new StringBuilder();
      int currentOffset = 0;
      for (CaseConversionRegion conversionRegion : myConversionRegions) {
        result.append(myReplacement, currentOffset, conversionRegion.start);
        String region = myReplacement.substring(conversionRegion.start, conversionRegion.end);
        result.append(conversionRegion.toUpperCase ? region.toUpperCase(Locale.getDefault()) : region.toLowerCase(Locale.getDefault()));
        currentOffset = conversionRegion.end;
      }
      result.append(myReplacement, currentOffset, myReplacement.length());
    }
    return result.toString();
  }

  private void startConversionForCharacter(boolean toUpperCase) {
    int currentOffset = myReplacement.length();
    CaseConversionRegion lastRegion = myConversionRegions.isEmpty() ? null : myConversionRegions.get(myConversionRegions.size() - 1);
    if (lastRegion == null || lastRegion.end >= 0 && lastRegion.end <= currentOffset) {
      myConversionRegions.add(new CaseConversionRegion(currentOffset, currentOffset + 1, toUpperCase));
    }
  }

  private void startConversionForRegion(boolean toUpperCase) {
    int currentOffset = myReplacement.length();
    CaseConversionRegion lastRegion = myConversionRegions.isEmpty() ? null : myConversionRegions.get(myConversionRegions.size() - 1);
    if (lastRegion == null) {
      myConversionRegions.add(new CaseConversionRegion(currentOffset, -1, toUpperCase));
    }
    else if (lastRegion.start == currentOffset) {
      lastRegion.end = -1;
      lastRegion.toUpperCase = toUpperCase;
    }
    else {
      if (lastRegion.end == -1) {
        if (lastRegion.toUpperCase == toUpperCase) {
          return;
        }
        lastRegion.end = currentOffset;
      }
      myConversionRegions.add(new CaseConversionRegion(currentOffset, -1, toUpperCase));
    }
  }

  private void resetConversionState() {
    if (!myConversionRegions.isEmpty()) {
      int currentOffset = myReplacement.length();
      int lastIndex = myConversionRegions.size() - 1;
      CaseConversionRegion lastRegion = myConversionRegions.get(lastIndex);
      if (lastRegion.start >= currentOffset) {
        myConversionRegions.remove(lastIndex);
      }
      else if (lastRegion.end == -1) {
        lastRegion.end = currentOffset;
      }
    }
  }

  private static boolean isLatinLetter(int ch) {
    return ((ch-'a')|('z'-ch)) >= 0 || ((ch-'A')|('Z'-ch)) >= 0;
  }

  private static boolean isDigit(int ch) {
    return ((ch-'0')|('9'-ch)) >= 0;
  }

  private static class CaseConversionRegion {
    private final int start;
    private int end;
    private boolean toUpperCase;

    private CaseConversionRegion(int start, int end, boolean toUpperCase) {
      this.start = start;
      this.end = end;
      this.toUpperCase = toUpperCase;
    }
  }
}
