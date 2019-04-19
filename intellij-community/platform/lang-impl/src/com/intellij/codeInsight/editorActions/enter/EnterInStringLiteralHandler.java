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

package com.intellij.codeInsight.editorActions.enter;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.EnterHandler;
import com.intellij.codeInsight.editorActions.JavaLikeQuoteHandler;
import com.intellij.codeInsight.editorActions.QuoteHandler;
import com.intellij.codeInsight.editorActions.TypedHandler;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lexer.StringLiteralLexer;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnterInStringLiteralHandler extends EnterHandlerDelegateAdapter {
  @Override
  public Result preprocessEnter(@NotNull final PsiFile file, @NotNull final Editor editor, @NotNull Ref<Integer> caretOffsetRef,
                                @NotNull final Ref<Integer> caretAdvanceRef, @NotNull final DataContext dataContext,
                                final EditorActionHandler originalHandler) {
    final Language language = EnterHandler.getLanguage(dataContext);
    if (language == null) return Result.Continue;
    
    int caretOffset = caretOffsetRef.get().intValue();
    final JavaLikeQuoteHandler quoteHandler = getJavaLikeQuoteHandler(editor, file);
    if (!isInStringLiteral(editor, quoteHandler, caretOffset)) {
      return Result.Continue;
    }

    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
    PsiElement psiAtOffset = file.findElementAt(caretOffset);
    if (psiAtOffset != null && psiAtOffset.getTextOffset() < caretOffset) {
      ASTNode token = psiAtOffset.getNode();
      if (quoteHandler.getConcatenatableStringTokenTypes().contains(token.getElementType())) {
        Document document = editor.getDocument();
        CharSequence text = document.getText();
        
        TextRange range = token.getTextRange();
        final char literalStart = token.getText().charAt(0);
        final StringLiteralLexer lexer = new StringLiteralLexer(literalStart, token.getElementType());
        lexer.start(text, range.getStartOffset(), range.getEndOffset());

        while (lexer.getTokenType() != null) {
          if (lexer.getTokenStart() < caretOffset && caretOffset < lexer.getTokenEnd()) {
            if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(lexer.getTokenType())) {
              caretOffset = lexer.getTokenEnd();
            }
            break;
          }
          lexer.advance();
        }

        int caretAdvance = caretAdvanceRef.get().intValue();        
        if (quoteHandler.needParenthesesAroundConcatenation(psiAtOffset)) {
          document.insertString(psiAtOffset.getTextRange().getEndOffset(), ")");
          document.insertString(psiAtOffset.getTextRange().getStartOffset(), "(");
          caretOffset++;
          caretAdvance++;
        }

        final String insertedFragment = literalStart + " " + quoteHandler.getStringConcatenationOperatorRepresentation();
        document.insertString(caretOffset, insertedFragment + " " + literalStart);
        caretOffset += insertedFragment.length();
        caretAdvance = 1;
        if (CodeStyle.getLanguageSettings(file).BINARY_OPERATION_SIGN_ON_NEXT_LINE) {
          caretOffset -= 1;
          caretAdvance = 3;
        }
        caretOffsetRef.set(caretOffset);
        caretAdvanceRef.set(caretAdvance);
        return Result.DefaultForceIndent;
      }
    }
    return Result.Continue;
  }

  @Nullable
  protected JavaLikeQuoteHandler getJavaLikeQuoteHandler(@NotNull Editor editor, @NotNull PsiElement psiAtOffset) {
    final QuoteHandler fileTypeQuoteHandler = TypedHandler.getQuoteHandler(psiAtOffset.getContainingFile(), editor);
    return fileTypeQuoteHandler instanceof JavaLikeQuoteHandler 
           ? (JavaLikeQuoteHandler)fileTypeQuoteHandler 
           : null;
  }

  @Contract("_,null,_->false")
  private static boolean isInStringLiteral(@NotNull Editor editor, @Nullable JavaLikeQuoteHandler quoteHandler, int offset) {
    if (offset > 0 && quoteHandler != null) {
      EditorHighlighter highlighter = ((EditorEx)editor).getHighlighter();
      HighlighterIterator iterator = highlighter.createIterator(offset - 1);
      final IElementType type = iterator.getTokenType();
      if ((StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(type) || quoteHandler.isInsideLiteral(iterator))
          && quoteHandler.getConcatenatableStringTokenTypes() != null) {
        return true;
      }
    }
    return false;
  }
}
