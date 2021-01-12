// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceSubSequence;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.IntStream;

import static com.intellij.util.ObjectUtils.notNull;

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


    TextRange realRange = ElementManipulators.getValueTextRange(host)
      .shiftRight(host.getTextRange().getStartOffset());
    if (!realRange.contains(cursorOffset)) return Collections.emptyList();

    PsiElement valueElement = findValueElement(host, realRange);

    realRange = limitToCurrentLineAndStripWhiteSpace(editorText, cursorOffset, realRange);

    BitSet charEscapeLocations = isWithinLiteral(e, host)
                                 ? findCharEscapeLocations(editor, editorText, host.getTextRange(), realRange.getStartOffset())
                                 : new BitSet(0);

    BitSet compositeIndexes = createCompositeIndexesSet(valueElement, realRange.getStartOffset());
    List<TextRange> segments = buildSegments(editorText, cursorOffset, realRange, charEscapeLocations, compositeIndexes);
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
  private static PsiElement findValueElement(@NotNull PsiElement host, @NotNull TextRange valueRange) {
    return notNull(
      PsiTreeUtil.findFirstParent(
        host.getContainingFile().findElementAt(valueRange.getStartOffset()), false,
        parent -> parent == host || parent.getTextRange().contains(valueRange)),
      host);
  }

  @NotNull
  private static List<TextRange> buildSegments(@NotNull CharSequence editorText,
                                               final int cursorOffset,
                                               @NotNull TextRange range,
                                               @NotNull BitSet charEscapeLocations,
                                               @NotNull BitSet compositeIndexes) {
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
      if (compositeIndexes.get(i - hostTextOffset)) {
        continue;
      }
      char ch = editorText.charAt(i);
      if (!segmentsFinished) {
        if (ch == '/'
            || (ch == '\\' && !charEscapeLocations.get(i - hostTextOffset))
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
          if (i + 1 < hostTextEndOffset) {
            segments.add(new TextRange(i + 1, hostTextEndOffset));
          }
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
  private static BitSet createCompositeIndexesSet(@NotNull PsiElement valueElement, int indexesOffset) {
    return StreamEx.of(valueElement.getChildren())
      .filter(child -> !(child instanceof LeafPsiElement))
      .map(PsiElement::getTextRange)
      .flatMapToInt(range -> IntStream.range(range.getStartOffset(), range.getEndOffset()))
      .map(index -> index - indexesOffset)
      .atLeast(0)
      .toBitSet();
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

  private static BitSet findCharEscapeLocations(@NotNull Editor editor,
                                                @NotNull CharSequence text,
                                                @NotNull TextRange range,
                                                int indexesOffset) {
    HighlighterIterator iterator =
      ((EditorEx)editor).getHighlighter().createIterator(range.getStartOffset());
    int rangeEnd = range.getEndOffset();

    BitSet locations = new BitSet(range.getLength());
    int pos;
    while (!iterator.atEnd() && (pos = iterator.getStart()) < rangeEnd) {
      if (text.charAt(pos) == '\\'
          && (pos + 1 >= rangeEnd || text.charAt(pos + 1) != '\\')
          && pos >= indexesOffset) {
        locations.set(pos - indexesOffset);
      }
      iterator.advance();
    }

    return locations;
  }
}
