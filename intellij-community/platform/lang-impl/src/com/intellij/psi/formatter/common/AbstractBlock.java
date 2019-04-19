/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.ASTNode;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class AbstractBlock implements ASTBlock, ExtraRangesProvider {
  public static final List<Block> EMPTY = Collections.emptyList();
  @NotNull protected final  ASTNode     myNode;
  @Nullable protected final Wrap        myWrap;
  @Nullable protected final Alignment   myAlignment;

  private List<Block> mySubBlocks;
  private Boolean myIncomplete;
  private boolean myBuildIndentsOnly = false;

  protected AbstractBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment) {
    myNode = node;
    myWrap = wrap;
    myAlignment = alignment;
  }

  @Override
  @NotNull
  public TextRange getTextRange() {
    return myNode.getTextRange();
  }

  @Override
  @NotNull
  public List<Block> getSubBlocks() {
    if (mySubBlocks == null) {
      List<Block> list = buildChildren();
      if (list.isEmpty()) {
        list = buildInjectedBlocks();
      }
      mySubBlocks = !list.isEmpty() ? list : EMPTY;
    }
    return mySubBlocks;
  }

  /**
   * Prevents from building injected blocks, which allows to build blocks faster
   * Initially was made for formatting-based indent detector
   */
  public void setBuildIndentsOnly(boolean value) {
    myBuildIndentsOnly = value;
  }

  protected boolean isBuildIndentsOnly() {
    return myBuildIndentsOnly;
  }

  @NotNull
  private List<Block> buildInjectedBlocks() {
    if (myBuildIndentsOnly) {
      return EMPTY;
    }
    if (!(this instanceof SettingsAwareBlock)) {
      return EMPTY;
    }
    PsiElement psi = myNode.getPsi();
    if (psi == null) {
      return EMPTY;
    }
    PsiFile file = psi.getContainingFile();
    if (file == null) {
      return EMPTY;
    }

    TextRange blockRange = myNode.getTextRange();
    List<DocumentWindow> documentWindows = InjectedLanguageManager.getInstance(file.getProject()).getCachedInjectedDocumentsInRange(file, blockRange);
    if (documentWindows.isEmpty()) {
      return EMPTY;
    }

    for (DocumentWindow documentWindow : documentWindows) {
      int startOffset = documentWindow.injectedToHost(0);
      int endOffset = startOffset + documentWindow.getTextLength();
      if (blockRange.containsRange(startOffset, endOffset)) {
        PsiFile injected = PsiDocumentManager.getInstance(psi.getProject()).getCachedPsiFile(documentWindow);
        if (injected != null) {
          List<Block> result = ContainerUtilRt.newArrayList();
          DefaultInjectedLanguageBlockBuilder builder = new DefaultInjectedLanguageBlockBuilder(((SettingsAwareBlock)this).getSettings());
          builder.addInjectedBlocks(result, myNode, getWrap(), getAlignment(), getIndent());
          return result;
        }
      }
    }
    return EMPTY;
  }

  protected abstract List<Block> buildChildren();

  @Nullable
  @Override
  public Wrap getWrap() {
    return myWrap;
  }

  @Override
  public Indent getIndent() {
    return null;
  }

  @Nullable
  @Override
  public Alignment getAlignment() {
    return myAlignment;
  }

  @NotNull
  @Override
  public ASTNode getNode() {
    return myNode;
  }

  @Override
  @NotNull
  public ChildAttributes getChildAttributes(final int newChildIndex) {
    return new ChildAttributes(getChildIndent(), getFirstChildAlignment());
  }

  @Nullable
  private Alignment getFirstChildAlignment() {
    List<Block> subBlocks = getSubBlocks();
    for (final Block subBlock : subBlocks) {
      Alignment alignment = subBlock.getAlignment();
      if (alignment != null) {
        return alignment;
      }
    }
    return null;
  }

  @Nullable
  protected Indent getChildIndent() {
    return null;
  }

  @Override
  public boolean isIncomplete() {
    if (myIncomplete == null) {
      myIncomplete = FormatterUtil.isIncomplete(getNode());
    }
    return myIncomplete;
  }

  @Override
  public String toString() {
    return myNode.getText() + " " + getTextRange();
  }

  /**
   * @return additional range to reformat, when this block if formatted
   */
  @Override
  @Nullable
  public List<TextRange> getExtraRangesToFormat(@NotNull FormattingRangesInfo info) {
    int startOffset = getTextRange().getStartOffset();
    if (info.isOnInsertedLine(startOffset) && myNode.textContains('\n')) {
      return new NodeIndentRangesCalculator(myNode).calculateExtraRanges();
    }
    return null;
  }
  
}