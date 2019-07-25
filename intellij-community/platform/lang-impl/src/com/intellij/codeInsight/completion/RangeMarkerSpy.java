// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public abstract class RangeMarkerSpy implements DocumentListener {
  private final RangeMarker myMarker;

  public RangeMarkerSpy(RangeMarker marker) {
    myMarker = marker;
    assert myMarker.isValid();
  }

  protected abstract void invalidated(DocumentEvent e);

  @Override
  public void documentChanged(@NotNull DocumentEvent e) {
    if (!myMarker.isValid()) {
      invalidated(e);
    }
  }
}
