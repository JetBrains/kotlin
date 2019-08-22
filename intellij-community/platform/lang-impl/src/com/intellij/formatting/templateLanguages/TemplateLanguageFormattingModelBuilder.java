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
package com.intellij.formatting.templateLanguages;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.DocumentBasedFormattingModel;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.intellij.formatting.templateLanguages.BlockUtil.buildChildWrappers;
import static com.intellij.formatting.templateLanguages.BlockUtil.filterBlocksByRange;

/**
 * @author Alexey Chmutov
 */
public abstract class TemplateLanguageFormattingModelBuilder implements DelegatingFormattingModelBuilder, TemplateLanguageBlockFactory {

  @Override
  @NotNull
  public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
    final PsiFile file = element.getContainingFile();
    Block rootBlock = getRootBlock(file, file.getViewProvider(), settings);
    return new DocumentBasedFormattingModel(rootBlock, element.getProject(), settings, file.getFileType(), file);
  }

  protected Block getRootBlock(PsiElement element, FileViewProvider viewProvider, CodeStyleSettings settings) {
    ASTNode node = element.getNode();
    if (node == null) {
      return createDummyBlock(node);
    }
    if (viewProvider instanceof TemplateLanguageFileViewProvider) {
      final Language dataLanguage = ((TemplateLanguageFileViewProvider)viewProvider).getTemplateDataLanguage();
      final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forLanguage(dataLanguage);
      if (builder instanceof DelegatingFormattingModelBuilder && ((DelegatingFormattingModelBuilder)builder).dontFormatMyModel(element)) {
        return createDummyBlock(node);
      }
      if (builder != null) {
        final FormattingModel model = builder.createModel(viewProvider.getPsi(dataLanguage), settings);
        List<DataLanguageBlockWrapper> childWrappers = buildChildWrappers(model.getRootBlock());
        if (childWrappers.size() == 1) {
          childWrappers = buildChildWrappers(childWrappers.get(0).getOriginal());
        }
        return createTemplateLanguageBlock(node, Wrap.createWrap(WrapType.NONE, false), null,
                                           filterBlocksByRange(childWrappers, node.getTextRange()), settings);
      }
    }
    return createTemplateLanguageBlock(node, Wrap.createWrap(WrapType.NONE, false), null, Collections.emptyList(), settings);
  }

  protected AbstractBlock createDummyBlock(final ASTNode node) {
    return new AbstractBlock(node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment()) {
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
    };
  }

  @Override
  public boolean dontFormatMyModel() {
    return true;
  }
}
