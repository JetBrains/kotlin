// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayModel;
import com.intellij.openapi.editor.VisualPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

class InlayModelWindow implements InlayModel {
  private static final Logger LOG = Logger.getInstance(InlayModelWindow.class);

  @Nullable
  @Override
  public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset,
                                                                           boolean relatesToPrecedingText,
                                                                           @NotNull T renderer) {
    logUnsupported();
    return null;
  }

  @Nullable
  @Override
  public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset,
                                                                          boolean relatesToPrecedingText,
                                                                          boolean showAbove,
                                                                          int priority,
                                                                          @NotNull T renderer) {
    logUnsupported();
    return null;
  }

  @Nullable
  @Override
  public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset,
                                                                                 boolean relatesToPrecedingText,
                                                                                 @NotNull T renderer) {
    logUnsupported();
    return null;
  }

  @NotNull
  @Override
  public List<Inlay> getInlineElementsInRange(int startOffset, int endOffset) {
    logUnsupported();
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<Inlay> getBlockElementsInRange(int startOffset, int endOffset) {
    logUnsupported();
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<Inlay> getBlockElementsForVisualLine(int visualLine, boolean above) {
    logUnsupported();
    return Collections.emptyList();
  }

  @Override
  public boolean hasInlineElementAt(int offset) {
    logUnsupported();
    return false;
  }

  @Nullable
  @Override
  public Inlay getInlineElementAt(@NotNull VisualPosition visualPosition) {
    logUnsupported();
    return null;
  }

  @Nullable
  @Override
  public Inlay getElementAt(@NotNull Point point) {
    logUnsupported();
    return null;
  }

  @NotNull
  @Override
  public List<Inlay> getAfterLineEndElementsInRange(int startOffset, int endOffset) {
    logUnsupported();
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<Inlay> getAfterLineEndElementsForLogicalLine(int logicalLine) {
    logUnsupported();
    return Collections.emptyList();
  }

  @Override
  public void setConsiderCaretPositionOnDocumentUpdates(boolean enabled) {
    logUnsupported();
  }

  @Override
  public void addListener(@NotNull Listener listener, @NotNull Disposable disposable) {
    logUnsupported();
  }

  private static void logUnsupported() {
    LOG.error("Inlay operations are not supported for injected editors");
  }
}
