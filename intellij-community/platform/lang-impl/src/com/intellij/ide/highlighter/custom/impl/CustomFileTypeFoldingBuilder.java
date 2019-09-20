/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.highlighter.custom.impl;

import com.intellij.codeInsight.highlighting.BraceMatcher;
import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.ide.highlighter.custom.CustomFileHighlighter;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yole
 */
public class CustomFileTypeFoldingBuilder extends CustomFoldingBuilder {

  @Override
  protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                          @NotNull PsiElement root,
                                          @NotNull Document document,
                                          boolean quick) {
    FileType fileType = root.getContainingFile().getFileType();
    if (!(fileType instanceof CustomSyntaxTableFileType)) {
      return;
    }
    CustomFileHighlighter highlighter = new CustomFileHighlighter(((CustomSyntaxTableFileType) fileType).getSyntaxTable());
    buildBraceMatcherBasedFolding(descriptors, root, document, highlighter);
  }

  public static void buildBraceMatcherBasedFolding(List<? super FoldingDescriptor> descriptors,
                                                   PsiElement root,
                                                   Document document,
                                                   SyntaxHighlighter highlighter) {
    LexerEditorHighlighter editorHighlighter = new LexerEditorHighlighter(highlighter, EditorColorsManager.getInstance().getGlobalScheme());
    editorHighlighter.setText(document.getText());
    FileType fileType = root.getContainingFile().getFileType();
    BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, root.getLanguage());
    TextRange totalRange = root.getTextRange();
    final HighlighterIterator iterator = editorHighlighter.createIterator(totalRange.getStartOffset());

    final LinkedList<Trinity<Integer, Integer, IElementType>> stack = new LinkedList<>();
    String editorText = document.getText();
    while (!iterator.atEnd() && iterator.getStart() < totalRange.getEndOffset()) {
      final Trinity<Integer, Integer, IElementType> last;
      if (braceMatcher.isLBraceToken(iterator, editorText, fileType) &&
          braceMatcher.isStructuralBrace(iterator, editorText, fileType)) {
        stack.addLast(Trinity.create(iterator.getStart(), iterator.getEnd(), iterator.getTokenType()));
      }
      else if (braceMatcher.isRBraceToken(iterator, editorText, fileType) &&
               braceMatcher.isStructuralBrace(iterator, editorText, fileType)
               && !stack.isEmpty() && braceMatcher.isPairBraces((last = stack.getLast()).third, iterator.getTokenType())) {
        stack.removeLast();
        TextRange range = new TextRange(last.first, iterator.getEnd());
        if (StringUtil.countChars(document.getText(range), '\n') >= 3) {
          descriptors.add(new FoldingDescriptor(root, range));
        }
      }
      iterator.advance();
    }
  }

  @Override
  protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    return "{...}";
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}
