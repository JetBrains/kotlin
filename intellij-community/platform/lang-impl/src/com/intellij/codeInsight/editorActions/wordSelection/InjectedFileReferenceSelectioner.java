// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    TextRange realRange = stripDelimiters(editorText, host.getTextRange());
    realRange = limitToCurrentLine(editorText, cursorOffset, realRange);
    // Some literals, like CSS contain quotes withing which there is actual file name. Strip the content around quotes.
    realRange = stripCodeAroundQuotesIfAny(editorText, cursorOffset, realRange);
    realRange = stripWhitespaces(editorText, realRange);
    boolean withinLiteral = host instanceof PsiLiteralValue
                            || host.getClass().getName().toLowerCase(Locale.US).contains("literal");
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
  private static TextRange stripDelimiters(@NotNull CharSequence text, @NotNull TextRange range) {
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    boolean changed = false;


    if (end - start > 4
        // Multiline Comments
        && ((StringUtil.startsWith(text, start, "/*")
             && StringUtil.endsWith(text, start, end, "*/"))
            // Groovy literal
            || (StringUtil.startsWith(text, start, "$/")
                && StringUtil.endsWith(text, start, end, "/$")))) {
      end -= 2;
      start += 2;
      changed = true;
    }
    else {
      // Literals - quote may be repeated like in Kotlin
      while (end - start > 2) {
        char firstChar = text.charAt(start);
        char lastChar = text.charAt(end - 1);
        if (firstChar == lastChar
            && (firstChar == '\'' || firstChar == '"' || firstChar == '`' || firstChar == '/')) {
          start++;
          end--;
          changed = true;
          // Groovy literal - if quoted with /, unquote it only once
          if (firstChar == '/') {
            break;
          }
        }
        else {
          break;
        }
      }
    }
    return changed ? new TextRange(start, end) : range;
  }

  @NotNull
  private static TextRange stripWhitespaces(@NotNull CharSequence text, @NotNull TextRange range) {
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    boolean changed = false;
    while (start < end && Character.isWhitespace(text.charAt(start))) {
      start++;
      changed = true;
    }
    while (start < end && Character.isWhitespace(text.charAt(end - 1))) {
      end--;
      changed = true;
    }
    return changed ? new TextRange(start, end) : range;
  }

  @NotNull
  private static TextRange limitToCurrentLine(@NotNull CharSequence text, int cursor, @NotNull TextRange range) {
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    boolean changed = false;
    if (start < cursor) {
      for (int i = cursor - 1; i >= start; i--) {
        char curChar = text.charAt(i);
        if (curChar == '\n' || curChar == '\r') {
          start = i + 1;
          changed = true;
          break;
        }
      }
    }
    if (cursor + 1 < end) {
      for (int i = cursor; i < end; i++) {
        char curChar = text.charAt(i);
        if (curChar == '\n' || curChar == '\r') {
          end = i;
          changed = true;
          break;
        }
      }
    }
    return changed ? new TextRange(start, end) : range;
  }

  @NotNull
  private static TextRange stripCodeAroundQuotesIfAny(@NotNull CharSequence text, int cursor, @NotNull TextRange range) {
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    Character quoteChar = null;
    for (int i = start; i < cursor; i++) {
      char ch = text.charAt(i);
      if (ch == '\'' || ch == '"') {
        quoteChar = ch;
        start = i + 1;
        break;
      }
    }
    if (quoteChar != null) {
      for (int i = end - 1; i > cursor; i--) {
        if (text.charAt(i) == quoteChar) {
          return new TextRange(start, i);
        }
      }
    }
    return range;
  }
}
