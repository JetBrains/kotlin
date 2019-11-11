// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.codeInsight.completion.SkipAutopopupInStrings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceSubSequence;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This selectioner tries to guess location of file segments within a particular element.
 * Querying for exact file references locations from PsiElement on EDT might be too expensive
 * and cause long freezes. If the selectioner improperly behaves with a specific language
 * construct please improve it.
 */
public class InjectedFileReferenceSelectioner extends AbstractWordSelectioner {

  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return PsiTreeUtil.getParentOfType(e, PsiLanguageInjectionHost.class) != null;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, final int cursorOffset, @NotNull Editor editor) {
    PsiElement host = PsiTreeUtil.getParentOfType(e, PsiLanguageInjectionHost.class);
    if (host == null) return Collections.emptyList();

    TextRange realRange = ElementManipulators.getValueTextRange(host).shiftRight(host.getTextRange().getStartOffset());
    realRange = limitToCurrentLineAndStripWhiteSpace(editorText, cursorOffset, realRange);

    Set<Integer> charEscapeLocations = isWithinLiteral(e, host) ? findCharEscapeLocations(editor, editorText, host.getTextRange())
                                                                : Collections.emptySet();

    List<TextRange> segments = buildSegments(editorText, cursorOffset, charEscapeLocations, realRange);
    if (!segments.isEmpty()) {
      int endOffsetAlignment = segments.get(segments.size() - 1).getEndOffset();
      for (ListIterator<TextRange> it = segments.listIterator(); it.hasNext(); ) {
        TextRange r = it.next();
        if (r.getEndOffset() > cursorOffset) continue;
        it.set(TextRange.create(r.getStartOffset(), endOffsetAlignment));
      }
      segments.add(realRange);
    }
    return segments;
  }

  @NotNull
  private static List<TextRange> buildSegments(@NotNull CharSequence editorText,
                                               final int cursorOffset,
                                               @NotNull Set<Integer> charEscapeLocations,
                                               @NotNull TextRange range) {
    if (range.getLength() == 0) {
      return Collections.emptyList();
    }
    int hostTextOffset = range.getStartOffset();
    int hostTextEndOffset = range.getEndOffset();

    List<TextRange> segments = new ArrayList<>();
    int rangeStart = hostTextOffset;
    boolean segmentsFinished = false;
    int hardSegmentCount = 0;

    for (int i = hostTextOffset; i < hostTextEndOffset; i++) {
      char ch = editorText.charAt(i);
      if (!segmentsFinished) {
        if (ch == '/'
            || (ch == '\\' && !charEscapeLocations.contains(i))
            //treat space as soft segment marker
            || (ch == ' ' && i <= cursorOffset)) {
          if (rangeStart < i) {
            if (editorText.charAt(i - 1) == ':') {
              segments.add(new TextRange(rangeStart, i - 1));
            }
            else {
              segments.add(new TextRange(rangeStart, i));
            }
            if (i > cursorOffset) {
              segmentsFinished = true;
            }
          }
          rangeStart = i + 1;
          if (ch != ' ') {
            hardSegmentCount++;
          }
        }
        // URLs - expand to content after '?' first, but count it as soft segment
        else if (ch == '?') {
          segments.add(new TextRange(rangeStart, i));
          segments.add(new TextRange(rangeStart, hostTextEndOffset));
          segmentsFinished = true;
        }
      }
      // In case of encountering XML tag begin/end return empty segments list
      if ((ch == '>' && i > hostTextOffset && editorText.charAt(i - 1) == '/')
          || (ch == '<' && i + 1 < hostTextEndOffset && editorText.charAt(i + 1) == '/')) {
        return Collections.emptyList();
      }
    }
    if (hardSegmentCount <= 0) {
      return Collections.emptyList();
    }
    if (!segmentsFinished && rangeStart < hostTextEndOffset) {
      segments.add(new TextRange(rangeStart, hostTextEndOffset));
    }
    return segments;
  }

  @NotNull
  private static TextRange limitToCurrentLineAndStripWhiteSpace(@NotNull CharSequence text, int cursor, @NotNull TextRange range) {

    int subsequenceOffset = range.getStartOffset();
    CharSequence rangeText = new CharSequenceSubSequence(text, subsequenceOffset, range.getEndOffset());

    //limit to current line
    int start = CharArrayUtil.shiftBackwardUntil(rangeText, cursor - subsequenceOffset, "\n\r") + 1;
    int end = CharArrayUtil.shiftForwardUntil(rangeText, cursor - subsequenceOffset, "\n\r");

    //strip whitespace
    start = CharArrayUtil.shiftForward(rangeText, start, cursor - subsequenceOffset, " \t");
    end = CharArrayUtil.shiftBackward(rangeText, cursor - subsequenceOffset, end - 1, " \t") + 1;

    return new TextRange(
      subsequenceOffset + start,
      subsequenceOffset + end
    );
  }

  private static boolean isWithinLiteral(@NotNull PsiElement e, PsiElement host) {
    return host instanceof PsiLiteralValue
           || SkipAutopopupInStrings.isInStringLiteral(e);
  }

  private static Set<Integer> findCharEscapeLocations(@NotNull Editor editor, @NotNull CharSequence text, @NotNull TextRange range) {
    HighlighterIterator iterator =
      ((EditorEx)editor).getHighlighter().createIterator(range.getStartOffset());
    int rangeEnd = range.getEndOffset();

    Set<Integer> locations = new HashSet<>();
    int pos;
    while (!iterator.atEnd() && (pos = iterator.getStart()) < rangeEnd) {
      if (text.charAt(pos) == '\\'
          && (pos + 1 >= rangeEnd || text.charAt(pos + 1) != '\\')) {
        locations.add(pos);
      }
      iterator.advance();
    }

    return locations;
  }
}
