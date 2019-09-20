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
package com.intellij.codeInsight.folding.impl;

import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringTokenizer;

/**
 * Performs {@code 'PSI element <-> signature'} mappings on the basis of the target PSI element's offsets.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 */
public class OffsetsElementSignatureProvider extends AbstractElementSignatureProvider {

  private static final String TYPE_MARKER = "e";
  
  @Override
  protected PsiElement restoreBySignatureTokens(@NotNull PsiFile file,
                                                @NotNull PsiElement parent,
                                                @NotNull String type,
                                                @NotNull StringTokenizer tokenizer,
                                                @Nullable StringBuilder processingInfoStorage)
  {
    if (!TYPE_MARKER.equals(type)) {
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format(
          "Stopping '%s' provider because given signature doesn't have expected type - can work with '%s' but got '%s'%n",
          getClass().getName(), TYPE_MARKER, type
        ));
      }
      return null;
    }
    int start;
    int end;
    try {
      start = Integer.parseInt(tokenizer.nextToken());
      end = Integer.parseInt(tokenizer.nextToken());
    }
    catch (NumberFormatException e) {
      return null;
    }
    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format("Parsed target offsets - [%d; %d)%n", start, end));
    }

    int index = 0;
    if (tokenizer.hasMoreTokens()) {
      try {
        index = Integer.parseInt(tokenizer.nextToken());
      }
      catch (NumberFormatException e) {
        // Do nothing
      }
    }
    Language language = file.getLanguage();
    if (tokenizer.hasMoreTokens()) {
      String languageId = tokenizer.nextToken();
      Language languageFromSignature = Language.findLanguageByID(languageId);
      if (languageFromSignature == null) {
        if (processingInfoStorage != null) {
          processingInfoStorage.append(String.format("Couldn't find language for id %s", languageId));
        }
      }
      else {
        language = languageFromSignature;
      }
    }

    FileViewProvider viewProvider = file.getViewProvider();
    PsiElement element = viewProvider.findElementAt(start, language);
    if (element == null) {
      return null;
    }
    PsiElement result = findElement(start, end, index, element, processingInfoStorage);
    if (result != null) {
      return result;
    }
    else if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format(
        "Failed to find an element by the given offsets for language %s. Started by the element '%s' (%s)",
        language, element, element.getText()
      ));
    }
    PsiFile psiFile = viewProvider.getPsi(language);
    if (psiFile == null) {
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Couldn't find PSI for language %s", language.toString()));
      }
      return null;
    }
    final PsiElement injectedStartElement = InjectedLanguageUtil.findElementAtNoCommit(psiFile, start);
    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format(
        "Trying to find injected element starting from the '%s'%s%n",
        injectedStartElement, injectedStartElement == null ? "" : String.format("(%s)", injectedStartElement.getText())
      ));
    }
    if (injectedStartElement != null && injectedStartElement != element) {
      return findElement(start, end, index, injectedStartElement, processingInfoStorage);
    }
    return null;
  }

  @Nullable
  private PsiElement findElement(int start, int end, int index, @NotNull PsiElement element, @Nullable StringBuilder processingInfoStorage) {
    TextRange range = element.getTextRange();
    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format("Starting processing from element '%s'. It's range is %s%n", element, range));
    }
    while (range != null && range.getStartOffset() == start && range.getEndOffset() < end) {
      element = element.getParent();
      if (element == null) {
        if (processingInfoStorage != null) {
          processingInfoStorage.append("Reached top of PSI tree");
        }
        return null;
      }
      range = element.getTextRange();
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Expanding element to '%s' and range to '%s'%n", element, range));
      }
    }
    if (range == null || range.getStartOffset() != start || range.getEndOffset() != end) {
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format(
          "Stopping %s because target element's range differs from the target one. Element: '%s', it's range: %s%n",
          getClass(), element, range));
      }
      return null;
    }

    // There is a possible case that we have a hierarchy of PSI elements that target the same document range. We need to find
    // out the right one then.

    int indexFromRoot = getElementHierarchyIndex(element);

    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format("Target element index is %d. Current index from root is %d%n", index, indexFromRoot));
    }

    if (index > indexFromRoot) {
      int steps = index - indexFromRoot;
      PsiElement result = element;
      for (PsiElement e = result.getFirstChild(); steps > 0 && e != null && range.equals(e.getTextRange()); steps--, e = e.getFirstChild()) {
        if (processingInfoStorage != null) {
          processingInfoStorage.append(String.format("Clarifying target element to '%s', its range is %s%n", result, result.getTextRange()));
        }
        result = e;
      }
      return result;
    }

    int steps = indexFromRoot - index;
    PsiElement result = element;
    while (--steps >= 0) {
      result = result.getParent();
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Reducing target element to '%s', its range is %s%n", result, result.getTextRange()));
      }
    }
    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format("Returning element '%s', its range is %s%n", result, result.getTextRange()));
    }
    return result;
  }

  @Override
  public String getSignature(@NotNull PsiElement element) {
    TextRange range = element.getTextRange();
    if (range.isEmpty()) {
      return null;
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append(TYPE_MARKER).append("#");
    buffer.append(range.getStartOffset());
    buffer.append(ELEMENT_TOKENS_SEPARATOR);
    buffer.append(range.getEndOffset());
    
    // There is a possible case that given PSI element has a parent or child that targets the same range. So, we remember
    // not only target range offsets but 'hierarchy index' as well.
    int index = getElementHierarchyIndex(element);

    buffer.append(ELEMENT_TOKENS_SEPARATOR).append(index);
    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null && containingFile.getViewProvider().getLanguages().size() > 1) {
      buffer.append(ELEMENT_TOKENS_SEPARATOR).append(containingFile.getLanguage().getID());
    }
    return buffer.toString();
  }

  private static int getElementHierarchyIndex(@NotNull PsiElement element) {
    TextRange range = element.getTextRange();
    int index = 0;
    for (PsiElement e = element.getParent(); e != null && range.equals(e.getTextRange()); e = e.getParent()) {
      index++;
    }
    return index;
  }
}
