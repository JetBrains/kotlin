/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInsight.highlighting;

import com.intellij.lang.BracePair;
import com.intellij.lang.Language;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PairedBraceMatcherAdapter implements NontrivialBraceMatcher, PairedBraceMatcher {
  private final PairedBraceMatcher myMatcher;
  private final Language myLanguage;

  public PairedBraceMatcherAdapter(@NotNull PairedBraceMatcher matcher, @NotNull Language language) {
    myMatcher = matcher;
    myLanguage = language;
  }

  @NotNull
  @Override
  public BracePair[] getPairs() {
    return myMatcher.getPairs();
  }

  @Override
  public int getBraceTokenGroupId(IElementType tokenType) {
    final BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == pair.getLeftBraceType() || tokenType == pair.getRightBraceType()) return myLanguage.hashCode();
    }
    return -1;
  }

  @Nullable
  public BracePair findPair(boolean left, HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
    final IElementType tokenType = iterator.getTokenType();
    final BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == (left ? pair.getLeftBraceType() : pair.getRightBraceType())) return pair;
    }
    return null;
  }

  @Override
  public boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
    return findPair(true, iterator, fileText, fileType) != null;
  }

  @Override
  public boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
    return findPair(false, iterator, fileText, fileType) != null;
  }

  @Override
  public IElementType getOppositeBraceTokenType(@NotNull final IElementType type) {
    final BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (type == pair.getRightBraceType()) return pair.getLeftBraceType();
      if (type == pair.getLeftBraceType()) return pair.getRightBraceType();
    }

    return null;
  }

  @Override
  public boolean isPairBraces(IElementType tokenType, IElementType tokenType2) {
    final BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == pair.getLeftBraceType() && tokenType2 == pair.getRightBraceType() ||
          tokenType == pair.getRightBraceType() && tokenType2 == pair.getLeftBraceType()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType) {
    final IElementType tokenType = iterator.getTokenType();
    final BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == pair.getRightBraceType() || tokenType == pair.getLeftBraceType()) return pair.isStructural();
    }
    return false;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    return myMatcher.isPairedBracesAllowedBeforeType(lbraceType, contextType);
  }

  @Override
  public int getCodeConstructStart(final PsiFile file, int openingBraceOffset) {
    return myMatcher.getCodeConstructStart(file, openingBraceOffset);
  }

  @Override
  @NotNull
  public List<IElementType> getOppositeBraceTokenTypes(@NotNull IElementType type) {
    List<IElementType> result = null;

    for (BracePair pair : myMatcher.getPairs()) {
      IElementType match = null;

      if (type == pair.getRightBraceType()) match = pair.getLeftBraceType();
      if (type == pair.getLeftBraceType()) match = pair.getRightBraceType();

      if (match != null) {
        if (result == null) result = new ArrayList<>(2);
        result.add(match);
      }
    }

    return result != null ? result : Collections.emptyList();
  }

  @Override
  public boolean shouldStopMatch(boolean forward, @NotNull IElementType braceType, @NotNull HighlighterIterator iterator) {
    if (myMatcher instanceof BraceMatcherTerminationAspect) {
      return ((BraceMatcherTerminationAspect)myMatcher).shouldStopMatch(forward, braceType, iterator);
    }
    return false;
  }
}
