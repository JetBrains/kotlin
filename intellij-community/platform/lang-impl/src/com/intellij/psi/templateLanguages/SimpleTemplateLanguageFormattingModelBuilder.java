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
package com.intellij.psi.templateLanguages;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.DocumentBasedFormattingModel;
import com.intellij.psi.formatter.common.AbstractBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class SimpleTemplateLanguageFormattingModelBuilder implements FormattingModelBuilder{
  @Override
  @NotNull
  public FormattingModel createModel(final PsiElement element, final CodeStyleSettings settings) {
    if (element instanceof PsiFile) {
      final FileViewProvider viewProvider = ((PsiFile)element).getViewProvider();
      if (viewProvider instanceof TemplateLanguageFileViewProvider) {
        final Language templateDataLanguage = ((TemplateLanguageFileViewProvider)viewProvider).getTemplateDataLanguage();
        FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forLanguage(templateDataLanguage);
        if (builder != null && templateDataLanguage != element.getLanguage()) {
          return builder.createModel(viewProvider.getPsi(templateDataLanguage), settings);
        }
      }
    }

    final PsiFile file = element.getContainingFile();
    return new DocumentBasedFormattingModel(new AbstractBlock(element.getNode(), Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment()) {
      @Override
      protected List<Block> buildChildren() {
        return Collections.emptyList();
      }

      @Override
      public Spacing getSpacing(final Block child1, @NotNull final Block child2) {
        return Spacing.getReadOnlySpacing();
      }

      @Override
      public boolean isLeaf() {
        return true;
      }
    }, element.getProject(), settings, file.getFileType(), file);
  }

}
