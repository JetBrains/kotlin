// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.RangeMarkerEx;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

class RangeMarkerWindow implements RangeMarkerEx {
  private final DocumentWindow myDocumentWindow;
  private final RangeMarkerEx myHostMarker;
  private final int myStartShift;
  private final int myEndShift;

  /**
   * Creates new {@code RangeMarkerWindow} object with the given data.
   * 
   * @param documentWindow  target document window
   * @param hostMarker      backing host range marker
   * @param startShift      there is a possible situation that injected fragment uses non-empty
   *                        {@link PsiLanguageInjectionHost.Shred#getPrefix() prefix} and
   *                        {@link PsiLanguageInjectionHost.Shred#getSuffix() suffix}. It's also possible that target
   *                        injected offsets are located at prefix/suffix space. We need to hold additional information
   *                        in order to perform {@code 'host -> injected'} mapping then. This argument specifies difference
   *                        between the start offset of the given host range marker at the injected text and target injected text
   *                        start offset
   * @param endShift        similar to the 'startShift' argument but specifies difference between the target injected host end offset
   *                        and end offset of the given host range marker at the injected text
   */
  RangeMarkerWindow(@NotNull DocumentWindow documentWindow, RangeMarkerEx hostMarker, int startShift, int endShift) {
    myDocumentWindow = documentWindow;
    myHostMarker = hostMarker;
    myStartShift = startShift;
    myEndShift = endShift;
  }

  @Override
  @NotNull
  public Document getDocument() {
    return myDocumentWindow;
  }

  @Override
  public int getStartOffset() {
    int hostOffset = myHostMarker.getStartOffset();
    return myDocumentWindow.hostToInjected(hostOffset) - myStartShift;
  }

  @Override
  public int getEndOffset() {
    int hostOffset = myHostMarker.getEndOffset();
    return myDocumentWindow.hostToInjected(hostOffset) + myStartShift + myEndShift;
  }

  @Override
  public boolean isValid() {
    if (!myHostMarker.isValid() || !myDocumentWindow.isValid()) return false;
    int startOffset = getStartOffset();
    int endOffset = getEndOffset();
    return startOffset <= endOffset && endOffset <= myDocumentWindow.getTextLength();
  }

  ////////////////////////////delegates
  @Override
  public void setGreedyToLeft(final boolean greedy) {
    myHostMarker.setGreedyToLeft(greedy);
  }

  @Override
  public void setGreedyToRight(final boolean greedy) {
    myHostMarker.setGreedyToRight(greedy);
  }

  @Override
  public <T> T getUserData(@NotNull final Key<T> key) {
    return myHostMarker.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull final Key<T> key, final T value) {
    myHostMarker.putUserData(key, value);
  }

  @Override
  public void documentChanged(@NotNull final DocumentEvent e) {
    myHostMarker.documentChanged(e);
  }
  @Override
  public long getId() {
    return myHostMarker.getId();
  }

  public RangeMarkerEx getDelegate() {
    return myHostMarker;
  }

  @Override
  public boolean isGreedyToRight() {
    return myHostMarker.isGreedyToRight();
  }

  @Override
  public boolean isGreedyToLeft() {
    return myHostMarker.isGreedyToLeft();
  }

  @Override
  public void dispose() {
    myHostMarker.dispose();
  }

  @Override
  public String toString() {
    return "RangeMarkerWindow" + (isGreedyToLeft() ? "[" : "(") + (isValid() ? "valid" : "invalid") + "," +
           getStartOffset() + "," + getEndOffset() + 
           (isGreedyToRight() ? "]" : ")") + " " + getId();
  }
}
