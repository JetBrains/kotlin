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

package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.highlighting.BraceMatcher;
import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public abstract class BraceMatcherBasedSelectioner extends ExtendWordSelectionHandlerBase {
  @Override
  public List<TextRange> select(@NotNull final PsiElement e, @NotNull final CharSequence editorText, final int cursorOffset, @NotNull final Editor editor) {
    final VirtualFile file = e.getContainingFile().getVirtualFile();
    final FileType fileType = file == null? null : file.getFileType();
    if (fileType == null) return super.select(e, editorText, cursorOffset, editor);
    final int textLength = editorText.length();
    final TextRange totalRange = e.getTextRange();
    final HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(totalRange.getStartOffset());
    final BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);

    final ArrayList<TextRange> result = new ArrayList<>();
    final LinkedList<Trinity<Integer, Integer, IElementType>> stack = new LinkedList<>();
    while (!iterator.atEnd() && iterator.getStart() < totalRange.getEndOffset()) {
      final Trinity<Integer, Integer, IElementType> last;
      if (braceMatcher.isLBraceToken(iterator, editorText, fileType)) {
        stack.addLast(Trinity.create(iterator.getStart(), iterator.getEnd(), iterator.getTokenType()));
      }
      else if (braceMatcher.isRBraceToken(iterator, editorText, fileType)
          && !stack.isEmpty() && braceMatcher.isPairBraces((last = stack.getLast()).third, iterator.getTokenType())) {
        stack.removeLast();
        result.addAll(expandToWholeLine(editorText, new TextRange(last.first, iterator.getEnd())));
        int bodyStart = last.second;
        int bodyEnd = iterator.getStart();
        while (bodyStart < textLength && Character.isWhitespace(editorText.charAt(bodyStart))) bodyStart++;
        while (bodyEnd > 0 && bodyStart < bodyEnd && Character.isWhitespace(editorText.charAt(bodyEnd - 1))) bodyEnd--;
        result.addAll(expandToWholeLine(editorText, new TextRange(bodyStart, bodyEnd)));
      }
      iterator.advance();
    }
    result.add(e.getTextRange());
    return result;
  }
}