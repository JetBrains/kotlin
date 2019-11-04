// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.codeInsight.completion.SkipAutopopupInStrings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceSubSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * This selectioner tries to guess location of file segments within a particular literal.
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
    boolean withinLiteral = host instanceof PsiLiteralValue
                            || SkipAutopopupInStrings.isInStringLiteral(host)
                            || SkipAutopopupInStrings.isInStringLiteral(e);
    List<TextRange> segments = buildSegments(editorText, cursorOffset, withinLiteral, realRange);

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
                                               boolean withinLiteral,
                                               @NotNull TextRange range) {
    if (range.getLength() == 0) {
      return Collections.emptyList();
    }
    int hostTextOffset = range.getStartOffset();
    int hostTextEndOffset = range.getEndOffset();

    ArrayList<TextRange> segments = new ArrayList<>();
    int rangeStart = hostTextOffset;
    boolean segmentsFinished = false;
    int hardSegmentCount = 0;
    for (int i = hostTextOffset; i < hostTextEndOffset; i++) {
      char ch = editorText.charAt(i);
      if (!segmentsFinished) {
        if (ch == '/'
            // in literals recognize only double '\' as path separator
            || (ch == '\\' && (!withinLiteral || (i + 1 < hostTextEndOffset && editorText.charAt(i + 1) == '\\')))
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
    int end = CharArrayUtil.shiftForwardUntil(rangeText, cursor-subsequenceOffset, "\n\r");

    //strip whitespace
    start = CharArrayUtil.shiftForward(rangeText, start, cursor-subsequenceOffset, " \t");
    end = CharArrayUtil.shiftBackward(rangeText, cursor-subsequenceOffset, end - 1, " \t") + 1;

    return new TextRange(
      subsequenceOffset + start,
      subsequenceOffset + end
    );
  }
}
