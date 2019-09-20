// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class LookupOffsets implements DocumentListener {
  @NotNull private String myAdditionalPrefix = "";

  private boolean myStableStart;
  @Nullable private Supplier<String> myStartMarkerDisposeInfo = null;
  @NotNull private RangeMarker myLookupStartMarker;
  private int myRemovedPrefix;
  private final RangeMarker myLookupOriginalStartMarker;
  private final Editor myEditor;

  public LookupOffsets(Editor editor) {
    myEditor = editor;
    int caret = getPivotOffset();
    myLookupOriginalStartMarker = createLeftGreedyMarker(caret);
    myLookupStartMarker = createLeftGreedyMarker(caret);
    myEditor.getDocument().addDocumentListener(this);
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent e) {
    if (myStartMarkerDisposeInfo == null && !myLookupStartMarker.isValid()) {
      Throwable throwable = new Throwable();
      String eString = e.toString();
      myStartMarkerDisposeInfo = () -> eString + "\n" + ExceptionUtil.getThrowableText(throwable);
    }
  }

  private RangeMarker createLeftGreedyMarker(int start) {
    RangeMarker marker = myEditor.getDocument().createRangeMarker(start, start);
    marker.setGreedyToLeft(true);
    return marker;
  }

  private int getPivotOffset() {
    return myEditor.getSelectionModel().hasSelection()
                 ? myEditor.getSelectionModel().getSelectionStart()
                 : myEditor.getCaretModel().getOffset();
  }

  @NotNull
  public String getAdditionalPrefix() {
    return myAdditionalPrefix;
  }

  public void appendPrefix(char c) {
    myAdditionalPrefix += c;
  }

  public boolean truncatePrefix() {
    final int len = myAdditionalPrefix.length();
    if (len == 0) {
      myRemovedPrefix++;
      return false;
    }
    myAdditionalPrefix = myAdditionalPrefix.substring(0, len - 1);
    return true;
  }

  void destabilizeLookupStart() {
    myStableStart = false;
  }

  void checkMinPrefixLengthChanges(Collection<? extends LookupElement> items, LookupImpl lookup) {
    if (myStableStart) return;
    if (!lookup.isCalculating() && !items.isEmpty()) {
      myStableStart = true;
    }

    int minPrefixLength = items.isEmpty() ? 0 : Integer.MAX_VALUE;
    for (final LookupElement item : items) {
      if (!(item instanceof EmptyLookupItem)) {
        minPrefixLength = Math.min(lookup.itemMatcher(item).getPrefix().length(), minPrefixLength);
      }
    }

    int start = getPivotOffset() - minPrefixLength - myAdditionalPrefix.length() + myRemovedPrefix;
    start = Math.max(Math.min(start, myEditor.getDocument().getTextLength()), 0);
    if (myLookupStartMarker.isValid() && myLookupStartMarker.getStartOffset() == start && myLookupStartMarker.getEndOffset() == start) {
      return;
    }

    myLookupStartMarker.dispose();
    myLookupStartMarker = createLeftGreedyMarker(start);
    myStartMarkerDisposeInfo = null;
  }

  int getLookupStart(@Nullable Throwable disposeTrace) {
    if (!myLookupStartMarker.isValid()) {
      throw new AssertionError(
        "Invalid lookup start: " + myLookupStartMarker + ", " + myEditor +
        ", disposeTrace=" + (disposeTrace == null ? null : ExceptionUtil.getThrowableText(disposeTrace)) +
        "\n================\n start dispose trace=" + (myStartMarkerDisposeInfo == null ? null : myStartMarkerDisposeInfo.get()));
    }
    return myLookupStartMarker.getStartOffset();
  }

  int getLookupOriginalStart() {
    return myLookupOriginalStartMarker.isValid() ? myLookupOriginalStartMarker.getStartOffset() : -1;
  }

  boolean performGuardedChange(Runnable change) {
    if (!myLookupStartMarker.isValid()) {
      throw new AssertionError("Invalid start: " + myEditor + ", trace=" + (myStartMarkerDisposeInfo == null ? null : myStartMarkerDisposeInfo
        .get()));
    }
    change.run();
    return myLookupStartMarker.isValid();
  }

  void clearAdditionalPrefix() {
    myAdditionalPrefix = "";
    myRemovedPrefix = 0;
  }

  void disposeMarkers() {
    myEditor.getDocument().removeDocumentListener(this);
    myLookupStartMarker.dispose();
    myLookupOriginalStartMarker.dispose();
  }

  public int getPrefixLength(LookupElement item, LookupImpl lookup) {
    return lookup.itemPattern(item).length() - myRemovedPrefix;
  }
}
