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
package com.intellij.psi.formatter.common;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class InjectedLanguageBlockBuilder {
  private static final Logger LOG = Logger.getInstance(InjectedLanguageBlockBuilder.class);

  @NotNull
  public Block createInjectedBlock(@NotNull ASTNode node,
                                   @NotNull Block originalBlock,
                                   Indent indent,
                                   int offset,
                                   TextRange range,
                                   @NotNull Language language) {
    return new InjectedLanguageBlockWrapper(originalBlock, offset, range, indent, language);
  }

  public abstract CodeStyleSettings getSettings();

  public abstract boolean canProcessFragment(String text, ASTNode injectionHost);

  public abstract Block createBlockBeforeInjection(ASTNode node, Wrap wrap, Alignment alignment, Indent indent, TextRange range);

  public abstract Block createBlockAfterInjection(ASTNode node, Wrap wrap, Alignment alignment, Indent indent, TextRange range);

  public boolean addInjectedBlocks(List<? super Block> result, final ASTNode injectionHost, Wrap wrap, Alignment alignment, Indent indent) {
    final Ref<Integer> lastInjectionEndOffset = new Ref<>(0);

    final PsiLanguageInjectionHost.InjectedPsiVisitor injectedPsiVisitor = (injectedPsi, places) -> {
      if (places.size() != 1) {
        return;
      }
      final PsiLanguageInjectionHost.Shred shred = places.get(0);
      TextRange injectionRange = shred.getRangeInsideHost();
      PsiLanguageInjectionHost shredHost = shred.getHost();
      if (shredHost == null) {
        return;
      }
      ASTNode node = shredHost.getNode();
      if (node == null || !injectionHost.getTextRange().contains(injectionRange.shiftRight(node.getStartOffset()))) {
        return;
      }
      if (node != injectionHost) {
        int shift = 0;
        boolean canProcess = false;
        for (ASTNode n = injectionHost.getTreeParent(), prev = injectionHost; n != null; prev = n, n = n.getTreeParent()) {
          shift += n.getStartOffset() - prev.getStartOffset();
          if (n == node) {
            injectionRange = injectionRange.shiftRight(shift);
            canProcess = true;
            break;
          }
        }
        if (!canProcess) {
          return;
        }
      }

      String childText;
      if (injectionHost.getTextLength() == injectionRange.getEndOffset() && injectionRange.getStartOffset() == 0 ||
          canProcessFragment((childText = injectionHost.getText()).substring(0, injectionRange.getStartOffset()), injectionHost) &&
          canProcessFragment(childText.substring(injectionRange.getEndOffset()), injectionHost)) {

        // inject language block

        final Language childLanguage = injectedPsi.getLanguage();
        final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(childLanguage, injectionHost.getPsi());

        if (builder != null) {
          final int startOffset = injectionRange.getStartOffset();
          final int endOffset = injectionRange.getEndOffset();
          TextRange range = injectionHost.getTextRange();
          final int prefixLength = shred.getPrefix().length();
          final int suffixLength = shred.getSuffix().length();

          int childOffset = range.getStartOffset();
          if (lastInjectionEndOffset.get() < startOffset) {
            result.add(createBlock(injectionHost, wrap, alignment, indent, new TextRange(lastInjectionEndOffset.get(), startOffset)));
          }

          addInjectedLanguageBlockWrapper(result, injectedPsi.getNode(), indent, childOffset + startOffset,
                                          new TextRange(prefixLength, injectedPsi.getTextLength() - suffixLength));

          lastInjectionEndOffset.set(endOffset);
        }
      }
    };
    final PsiElement injectionHostPsi = injectionHost.getPsi();
    PsiFile containingFile = injectionHostPsi.getContainingFile();
    InjectedLanguageManager.getInstance(containingFile.getProject())
      .enumerateEx(injectionHostPsi, containingFile, true, injectedPsiVisitor);

    if (lastInjectionEndOffset.get() > 0) {
      if (lastInjectionEndOffset.get() < injectionHost.getTextLength()) {
        result.add(createBlock(injectionHost, wrap, alignment, indent,
                               new TextRange(lastInjectionEndOffset.get(), injectionHost.getTextLength())));
      }
      return true;
    }
    return false;
  }

  private Block createBlock(ASTNode injectionHost, Wrap wrap, Alignment alignment, Indent indent, TextRange range) {
    if (range.getStartOffset() == 0) {
      final ASTNode leaf = injectionHost.findLeafElementAt(range.getEndOffset() - 1);
      return createBlockBeforeInjection(
        leaf, wrap, alignment, indent, range.shiftRight(injectionHost.getStartOffset()));
    }
    final ASTNode leaf = injectionHost.findLeafElementAt(range.getStartOffset());
    return createBlockAfterInjection(
      leaf, wrap, alignment, indent, range.shiftRight(injectionHost.getStartOffset()));
  }

  public void addInjectedLanguageBlockWrapper(final List<? super Block> result, final ASTNode injectedNode,
                                              final Indent indent, int offset, @Nullable TextRange range) {

    //
    // Do not create a block for an empty range
    //
    if (range != null) {
      if (range.getLength() == 0) return;
      if (StringUtil.isEmptyOrSpaces(range.substring(injectedNode.getText()))) {
        return;
      }
    }

    final PsiElement childPsi = injectedNode.getPsi();
    final Language childLanguage = childPsi.getLanguage();
    final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(childLanguage, childPsi);
    LOG.assertTrue(builder != null);
    final FormattingModel childModel = builder.createModel(childPsi, getSettings());
    Block original = childModel.getRootBlock();

    if (original.isLeaf() && !injectedNode.getText().trim().isEmpty() || !original.getSubBlocks().isEmpty()) {
      result.add(createInjectedBlock(injectedNode, original, indent, offset, range, childLanguage));
    }
  }
}
