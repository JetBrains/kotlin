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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.CustomHighlighterTokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;
import static com.intellij.patterns.StandardPatterns.instanceOf;

/**
 * @author yole
 */
public class CustomFileTypeCompletionContributor extends CompletionContributor implements DumbAware {
  public CustomFileTypeCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().inFile(psiFile().withFileType(instanceOf(CustomSyntaxTableFileType.class))),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               if (inCommentOrLiteral(parameters)) {
                 return;
               }

               FileType fileType = parameters.getOriginalFile().getFileType();
               if (!(fileType instanceof CustomSyntaxTableFileType)) {
                 return;
               }

               SyntaxTable syntaxTable = ((CustomSyntaxTableFileType)fileType).getSyntaxTable();
               String prefix = CompletionUtil.findJavaIdentifierPrefix(parameters);
               if (prefix.isEmpty() && parameters.isAutoPopup()) {
                 return;
               }

               CompletionResultSet resultSetWithPrefix = result.withPrefixMatcher(prefix);

               addVariants(resultSetWithPrefix, syntaxTable.getKeywords1());
               addVariants(resultSetWithPrefix, syntaxTable.getKeywords2());
               addVariants(resultSetWithPrefix, syntaxTable.getKeywords3());
               addVariants(resultSetWithPrefix, syntaxTable.getKeywords4());
               
               WordCompletionContributor.addWordCompletionVariants(resultSetWithPrefix, parameters, Collections.emptySet());
             }
           });
  }

  private static boolean inCommentOrLiteral(CompletionParameters parameters) {
    HighlighterIterator iterator = ((EditorEx)parameters.getEditor()).getHighlighter().createIterator(parameters.getOffset());
    if (iterator.atEnd()) return false;

    IElementType elementType = iterator.getTokenType();
    if (elementType == CustomHighlighterTokenType.WHITESPACE) {
      iterator.retreat();
      elementType = iterator.getTokenType();
    }
    return elementType == CustomHighlighterTokenType.LINE_COMMENT ||
           elementType == CustomHighlighterTokenType.MULTI_LINE_COMMENT ||
           elementType == CustomHighlighterTokenType.STRING ||
           elementType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING;
  }

  private static void addVariants(CompletionResultSet resultSet, Set<String> keywords) {
    for (String keyword : keywords) {
      resultSet.addElement(LookupElementBuilder.create(keyword).bold());
    }
  }

}
