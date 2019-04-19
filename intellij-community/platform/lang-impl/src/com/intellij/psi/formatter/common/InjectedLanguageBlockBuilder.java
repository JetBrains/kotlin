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

/**
 * @author nik
 */
public abstract class InjectedLanguageBlockBuilder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.formatter.xml.XmlInjectedLanguageBlockBuilder");

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

  public boolean addInjectedBlocks(List<Block> result, final ASTNode injectionHost, Wrap wrap, Alignment alignment, Indent indent) {
    final PsiFile[] injectedFile = new PsiFile[1];
    final Ref<TextRange> injectedRangeInsideHost = new Ref<>();
    final Ref<Integer> prefixLength = new Ref<>();
    final Ref<Integer> suffixLength = new Ref<>();
    final Ref<ASTNode> injectionHostToUse = new Ref<>(injectionHost);

    final PsiLanguageInjectionHost.InjectedPsiVisitor injectedPsiVisitor = (injectedPsi, places) -> {
      if (places.size() != 1) {
        return;
      }
      final PsiLanguageInjectionHost.Shred shred = places.get(0);
      TextRange textRange = shred.getRangeInsideHost();
      PsiLanguageInjectionHost shredHost = shred.getHost();
      if (shredHost == null) {
        return;
      }
      ASTNode node = shredHost.getNode();
      if (node == null || !injectionHost.getTextRange().contains(textRange.shiftRight(node.getStartOffset()))) {
        return;
      }
      if (node != injectionHost) {
        int shift = 0;
        boolean canProcess = false;
        for (ASTNode n = injectionHost.getTreeParent(), prev = injectionHost; n != null; prev = n, n = n.getTreeParent()) {
          shift += n.getStartOffset() - prev.getStartOffset();
          if (n == node) {
            textRange = textRange.shiftRight(shift);
            canProcess = true;
            break;
          }
        }
        if (!canProcess) {
          return;
        }
      }

      String childText;
        if (injectionHost.getTextLength() == textRange.getEndOffset() && textRange.getStartOffset() == 0 ||
            canProcessFragment((childText = injectionHost.getText()).substring(0, textRange.getStartOffset()), injectionHost) &&
            canProcessFragment(childText.substring(textRange.getEndOffset()), injectionHost)) {
          injectedFile[0] = injectedPsi;
          injectedRangeInsideHost.set(textRange);
          prefixLength.set(shred.getPrefix().length());
          suffixLength.set(shred.getSuffix().length());
        }
    };
    final PsiElement injectionHostPsi = injectionHost.getPsi();
    PsiFile containingFile = injectionHostPsi.getContainingFile();
    InjectedLanguageManager.getInstance(containingFile.getProject()).enumerateEx(injectionHostPsi, containingFile, true, injectedPsiVisitor);

    if  (injectedFile[0] != null) {
      final Language childLanguage = injectedFile[0].getLanguage();
      final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(childLanguage, injectionHostPsi);

      if (builder != null) {
        final int startOffset = injectedRangeInsideHost.get().getStartOffset();
        final int endOffset = injectedRangeInsideHost.get().getEndOffset();
        TextRange range = injectionHostToUse.get().getTextRange();

        int childOffset = range.getStartOffset();
        if (startOffset != 0) {
          final ASTNode leaf = injectionHostToUse.get().findLeafElementAt(startOffset - 1);
          result.add(createBlockBeforeInjection(leaf, wrap, alignment, indent, new TextRange(childOffset, childOffset + startOffset)));
        }

        addInjectedLanguageBlockWrapper(result, injectedFile[0].getNode(), indent, childOffset + startOffset,
                                        new TextRange(prefixLength.get(), injectedFile[0].getTextLength() - suffixLength.get()));

        if (endOffset != injectionHostToUse.get().getTextLength()) {
          final ASTNode leaf = injectionHostToUse.get().findLeafElementAt(endOffset);
          result.add(createBlockAfterInjection(leaf, wrap, alignment, indent, new TextRange(childOffset + endOffset, range.getEndOffset())));
        }
        return true;
      }
    }
    return false;
  }

  public void addInjectedLanguageBlockWrapper(final List<Block> result, final ASTNode injectedNode,
                                              final Indent indent, int offset, @Nullable TextRange range) {

    //
    // Do not create a block for an empty range
    //
    if (range != null) {
      if (range.getLength() == 0) return;
      if(StringUtil.isEmptyOrSpaces(range.substring(injectedNode.getText()))) {
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
