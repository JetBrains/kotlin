// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.util.containers.FList;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * @author peter
*/
public class CamelHumpMatcher extends PrefixMatcher {
  private final MinusculeMatcher myMatcher;
  private final MinusculeMatcher myCaseInsensitiveMatcher;
  private final boolean myCaseSensitive;
  private static boolean ourForceStartMatching;
  private final boolean myTypoTolerant;


  public CamelHumpMatcher(@NotNull final String prefix) {
    this(prefix, true);
  }

  public CamelHumpMatcher(String prefix, boolean caseSensitive) {
    this(prefix, caseSensitive, false);
  }

  CamelHumpMatcher(String prefix, boolean caseSensitive, boolean typoTolerant) {
    super(prefix);
    myCaseSensitive = caseSensitive;
    myTypoTolerant = typoTolerant;
    myMatcher = createMatcher(myCaseSensitive);
    myCaseInsensitiveMatcher = createMatcher(false);
  }

  @Override
  public boolean isStartMatch(String name) {
    return myMatcher.isStartMatch(name);
  }

  @Override
  public boolean isStartMatch(LookupElement element) {
    for (String s : CompletionUtil.iterateLookupStrings(element)) {
      FList<TextRange> ranges = myCaseInsensitiveMatcher.matchingFragments(s);
      if (ranges == null) continue;
      if (ranges.isEmpty() || skipUnderscores(s) >= ranges.get(0).getStartOffset()) {
        return true;
      }
    }

    return false;
  }

  boolean isTypoTolerant() {
    return myTypoTolerant;
  }

  private static int skipUnderscores(@NotNull String name) {
    return CharArrayUtil.shiftForward(name, 0, "_");
  }

  @Override
  public boolean prefixMatches(@NotNull final String name) {
    if (name.startsWith("_") &&
        CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE == CodeInsightSettings.FIRST_LETTER &&
        firstLetterCaseDiffers(name)) {
      return false;
    }

    return myMatcher.matches(name);
  }

  private boolean firstLetterCaseDiffers(String name) {
    int nameFirst = skipUnderscores(name);
    int prefixFirst = skipUnderscores(myPrefix);
    return nameFirst < name.length() &&
           prefixFirst < myPrefix.length() &&
           caseDiffers(name.charAt(nameFirst), myPrefix.charAt(prefixFirst));
  }

  private static boolean caseDiffers(char c1, char c2) {
    return Character.isLowerCase(c1) != Character.isLowerCase(c2) || Character.isUpperCase(c1) != Character.isUpperCase(c2);
  }

  @Override
  public boolean prefixMatches(@NotNull final LookupElement element) {
    return prefixMatchersInternal(element, !element.isCaseSensitive());
  }

  private boolean prefixMatchersInternal(final LookupElement element, final boolean itemCaseInsensitive) {
    for (final String name : element.getAllLookupStrings()) {
      if (itemCaseInsensitive && StringUtil.startsWithIgnoreCase(name, myPrefix) || prefixMatches(name)) {
        return true;
      }
      if (itemCaseInsensitive && CodeInsightSettings.ALL != CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE) {
        if (myCaseInsensitiveMatcher.matches(name)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  @NotNull
  public PrefixMatcher cloneWithPrefix(@NotNull final String prefix) {
    if (prefix.equals(myPrefix)) {
      return this;
    }
    
    return new CamelHumpMatcher(prefix, myCaseSensitive, myTypoTolerant);
  }

  private MinusculeMatcher createMatcher(final boolean caseSensitive) {
    String prefix = applyMiddleMatching(myPrefix);

    NameUtil.MatcherBuilder builder = NameUtil.buildMatcher(prefix);
    if (caseSensitive) {
      int setting = CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE;
      if (setting == CodeInsightSettings.FIRST_LETTER) {
        builder = builder.withCaseSensitivity(NameUtil.MatchingCaseSensitivity.FIRST_LETTER);
      }
      else if (setting == CodeInsightSettings.ALL) {
        builder = builder.withCaseSensitivity(NameUtil.MatchingCaseSensitivity.ALL);
      }
    }
    if (myTypoTolerant) {
      builder = builder.typoTolerant();
    }
    return builder.build();
  }

  public static String applyMiddleMatching(String prefix) {
    if (Registry.is("ide.completion.middle.matching") && !prefix.isEmpty() && !ourForceStartMatching) {
      return "*" + StringUtil.replace(prefix, ".", ". ").trim();
    }
    return prefix;
  }

  @Override
  public String toString() {
    return myPrefix;
  }

  /**
   * @deprecated In an ideal world, all tests would use the same settings as production, i.e. middle matching.
   * If you see a usage of this method which can be easily removed (i.e. it's easy to make a test pass without it
   * by modifying test expectations slightly), please do it
   */
  @TestOnly
  @Deprecated
  public static void forceStartMatching(Disposable parent) {
    ourForceStartMatching = true;
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourForceStartMatching = false;
      }
    });
  }

  @Override
  public int matchingDegree(String string) {
    return matchingDegree(string, matchingFragments(string));
  }

  @Nullable
  public FList<TextRange> matchingFragments(String string) {
    return myMatcher.matchingFragments(string);
  }

  public int matchingDegree(String string, @Nullable FList<? extends TextRange> fragments) {
    int underscoreEnd = skipUnderscores(string);
    if (underscoreEnd > 0) {
      FList<TextRange> ciRanges = myCaseInsensitiveMatcher.matchingFragments(string);
      if (ciRanges != null && !ciRanges.isEmpty()) {
        int matchStart = ciRanges.get(0).getStartOffset();
        if (matchStart > 0 && matchStart <= underscoreEnd) {
          return myCaseInsensitiveMatcher.matchingDegree(string.substring(matchStart), true) - 1;
        }
      }
    }

    return myMatcher.matchingDegree(string, true, fragments);
  }
}
