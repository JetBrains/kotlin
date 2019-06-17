// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractBlock implements ASTBlock, ExtraRangesProvider {
  public static final List<Block> EMPTY = Collections.emptyList();

  protected final @NotNull ASTNode myNode;
  protected final @Nullable Wrap myWrap;
  protected final @Nullable Alignment myAlignment;

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
   * Disables building injected blocks, which allows faster formatting-based indent detection.
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
          List<Block> result = new ArrayList<>();
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
  public ChildAttributes getChildAttributes(int newChildIndex) {
    return new ChildAttributes(getChildIndent(), getFirstChildAlignment());
  }

  @Nullable
  private Alignment getFirstChildAlignment() {
    List<Block> subBlocks = getSubBlocks();
    for (Block subBlock : subBlocks) {
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

  /**
   * @return additional range to reformat, when this block if formatted
   */
  @Nullable
  @Override
  public List<TextRange> getExtraRangesToFormat(@NotNull FormattingRangesInfo info) {
    return info.isOnInsertedLine(getTextRange().getStartOffset()) && myNode.textContains('\n')
           ? new NodeIndentRangesCalculator(myNode).calculateExtraRanges()
           : null;
  }

  @Override
  public String toString() {
    return myNode.getText() + " " + getTextRange();
  }
}