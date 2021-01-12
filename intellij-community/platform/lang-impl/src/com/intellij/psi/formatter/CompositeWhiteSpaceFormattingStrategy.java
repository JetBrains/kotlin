/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.psi.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Allows to combine multiple {@link WhiteSpaceFormattingStrategy} implementations.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 */
public class CompositeWhiteSpaceFormattingStrategy implements WhiteSpaceFormattingStrategy {

  private final List<WhiteSpaceFormattingStrategy> myStrategies = new ArrayList<>();
  private boolean myReplaceDefaultStrategy;

  public CompositeWhiteSpaceFormattingStrategy(@NotNull Collection<? extends WhiteSpaceFormattingStrategy> strategies)
    throws IllegalArgumentException
  {
    for (WhiteSpaceFormattingStrategy strategy : strategies) {
      addStrategy(strategy);
    }
  }

  @Override
  public int check(@NotNull CharSequence text, int start, int end) {
    int offset = start;
    while (offset < end) {
      int oldOffset = offset;
      for (WhiteSpaceFormattingStrategy strategy : myStrategies) {
        offset = strategy.check(text, offset, end);
        if (offset > oldOffset) {
          break;
        }
      }
      if (offset == oldOffset) {
        return start;
      }
    }
    return offset;
  }

  @Override
  public boolean replaceDefaultStrategy() {
    return myReplaceDefaultStrategy;
  }

  public void addStrategy(@NotNull WhiteSpaceFormattingStrategy strategy) throws IllegalArgumentException {
    if (myReplaceDefaultStrategy && strategy.replaceDefaultStrategy()) {
      throw new IllegalArgumentException(String.format(
        "Can't combine strategy '%s' with already registered strategies (%s). Reason: given strategy is marked to replace "
        + "all existing strategies but strategy with such characteristics is already registered", strategy, myStrategies
      ));
    }
    myStrategies.add(strategy);
    myReplaceDefaultStrategy |= strategy.replaceDefaultStrategy();
  }

  @NotNull
  @Override
  public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText,
                                                  @NotNull CharSequence text,
                                                  int startOffset,
                                                  int endOffset, CodeStyleSettings codeStyleSettings, ASTNode nodeAfter)
  {
    CharSequence result = whiteSpaceText;
    for (WhiteSpaceFormattingStrategy strategy : myStrategies) {
      result = strategy.adjustWhiteSpaceIfNecessary(result, text, startOffset, endOffset, codeStyleSettings, nodeAfter);
    }
    return result;
  }

  @Override
  public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText,
                                                  @NotNull PsiElement startElement,
                                                  int startOffset,
                                                  int endOffset, CodeStyleSettings codeStyleSettings)
  {
    CharSequence result = whiteSpaceText;
    for (WhiteSpaceFormattingStrategy strategy : myStrategies) {
      result = strategy.adjustWhiteSpaceIfNecessary(result, startElement, startOffset, endOffset, codeStyleSettings);
    }
    return result;
  }

  @Override
  public boolean containsWhitespacesOnly(@NotNull ASTNode node) {
    for (WhiteSpaceFormattingStrategy strategy : myStrategies) {
      if (strategy.containsWhitespacesOnly(node)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean addWhitespace(@NotNull ASTNode treePrev, @NotNull LeafElement whiteSpaceElement) {
    return false;
  }
}
