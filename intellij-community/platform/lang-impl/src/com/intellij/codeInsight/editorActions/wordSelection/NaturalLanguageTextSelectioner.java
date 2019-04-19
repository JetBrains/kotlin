/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPlainText;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NaturalLanguageTextSelectioner extends ExtendWordSelectionHandlerBase {
  private static final Set<Character> NATURAL = ContainerUtil.newTroveSet(
    '(', ')', '.', ',', ':', ';', '!', '?', '$', '@', '%', '\"', '\'', '<', '>', '[', ']', '_'
  );
  private static final Set<Character> SENTENCE_END = ContainerUtil.newTroveSet('.', '!', '?');

  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return (e instanceof PsiPlainText || e instanceof PsiComment) &&
           !(e.getContainingFile().getFileType() instanceof CustomSyntaxTableFileType);
  }

  private static TextRange findParagraphRange(String text, int start, int end) {
    int paragraphStart = text.lastIndexOf("\n\n", start);
    int paragraphEnd = text.indexOf("\n\n", end);
    if (paragraphStart >= paragraphEnd) {
      return new TextRange(0, text.length());
    }
    return new TextRange(paragraphStart >= 0 ? paragraphStart + 2 : 0, paragraphEnd < 0 ? text.length() : paragraphEnd);
  }

  @Nullable
  private static TextRange findCustomRange(String text, int start, int end, char startChar, char endChar) {
    int prev = text.lastIndexOf(startChar, start);
    int next = text.indexOf(endChar, end);
    if (prev < 0 || next < 0) {
      return null;
    }
    if (StringUtil.contains(text, prev + 1, start, endChar) || StringUtil.contains(text, end, next, startChar)) return null;
    if (prev + 1 < start || next > end) {
      return new TextRange(prev + 1, next);
    }

    return new TextRange(prev, next + 1);
  }

  @Nullable
  private static TextRange findSentenceRange(String editorText, int start, int end) {
    int sentenceStart = start;

    while (sentenceStart > 0) {
      if (start - sentenceStart > 1000) return null;
      if (isSentenceEnd(editorText, sentenceStart - 1) || !isNatural(editorText.charAt(sentenceStart - 1))) {
        break;
      }
      sentenceStart--;
    }
    while (sentenceStart < end && Character.isWhitespace(editorText.charAt(sentenceStart))) {
      sentenceStart++;
    }

    int sentenceEnd = Math.max(0, end - 1);

    while (sentenceEnd < editorText.length()) {
      sentenceEnd++;
      if (sentenceEnd - end > 1000) return null;
      if (isSentenceEnd(editorText, sentenceEnd - 1)) {
        break;
      }
    }
    return new TextRange(sentenceStart, sentenceEnd);
  }

  private static boolean isSentenceEnd(String text, final int i) {
    return SENTENCE_END.contains(text.charAt(i)) && (i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)));
  }

  private static TextRange findNaturalRange(String editorText, int start, int end) {
    while (start > 0) {
      if (!isNatural(editorText.charAt(start - 1))) {
        break;
      }
      start--;
    }

    while (end < editorText.length()) {
      final char c = editorText.charAt(end);
      if (!isNatural(c)) {
        break;
      }
      end++;
    }
    return new TextRange(start, end);
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    ArrayList<TextRange> result = new ArrayList<>();

    addWordRange(editorText, cursorOffset, result);

    TextRange range = expandSelection(e, editorText, cursorOffset, cursorOffset);
    if (range != null) {
      result.add(range);
      while (true) {
        TextRange next = expandSelection(e, editorText, range.getStartOffset(), range.getEndOffset());
        if (next == null || range.contains(next)) {
          break;
        }
        result.add(next);
        range = next;
      }
    }

    return result;
 }

  private static void addWordRange(CharSequence editorText, int cursorOffset, ArrayList<TextRange> result) {
    SelectWordUtil.addWordSelection(false, editorText, cursorOffset, result,
                                    ch -> Character.isJavaIdentifierPart(ch) || ch == '\'');
    SelectWordUtil.addWordSelection(false, editorText, cursorOffset, result,
                                    ch -> Character.isJavaIdentifierPart(ch) || ch == '\'' || ch == '-');
  }

  @Nullable
  private static TextRange expandSelection(PsiElement e, CharSequence editorText, int selStart, int selEnd) {
    TextRange range = e.getTextRange();
    int shift = range.getStartOffset();
    if (selStart <= shift || selEnd >= range.getEndOffset()) {
      return null;
    }

    String elementText = editorText.subSequence(shift, range.getEndOffset()).toString();
    int start = selStart - shift;
    int end = selEnd - shift;

    TextRange best = findSentenceRange(elementText, start, end);
    if (best == null) return null;

    best = narrowRange(best, findCustomRange(elementText, start, end, '\"', '\"'));
    best = narrowRange(best, findCustomRange(elementText, start, end, '(', ')'));
    best = narrowRange(best, findCustomRange(elementText, start, end, '<', '>'));
    best = narrowRange(best, findCustomRange(elementText, start, end, '[', ']'));

    TextRange natural = findNaturalRange(elementText, start, end);
    if (!natural.contains(best)) {
      return null;
    }

    TextRange paragraph = findParagraphRange(elementText, start, end);
    if (best.getStartOffset() == start && best.getEndOffset() == end || !paragraph.contains(best)) {
      return paragraph.shiftRight(shift);
    }


    return best.shiftRight(shift);
  }

  private static TextRange narrowRange(TextRange best, TextRange candidate) {
    return candidate != null && best.contains(candidate) ? candidate : best;
  }

  private static boolean isNatural(char c) {
    return Character.isWhitespace(c) || Character.isLetterOrDigit(c) || NATURAL.contains(c);
  }
}
