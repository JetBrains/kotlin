// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.editor.ex.RangeMarkerEx;
import com.intellij.openapi.editor.impl.FoldRegionImpl;
import org.jetbrains.annotations.NotNull;

public class FoldingRegionWindow extends RangeMarkerWindow implements FoldRegion {
  private final EditorWindow myEditorWindow;

  private final FoldRegion myHostRegion;

  FoldingRegionWindow(@NotNull DocumentWindow documentWindow,
                      @NotNull EditorWindow editorWindow,
                      @NotNull FoldRegion hostRegion,
                      int startShift,
                      int endShift)
  {
    super(documentWindow, (RangeMarkerEx)hostRegion, startShift, endShift);
    myEditorWindow = editorWindow;
    myHostRegion = hostRegion;
  }

  @Override
  public boolean isExpanded() {
    return myHostRegion.isExpanded();
  }

  @Override
  public void setExpanded(boolean expanded) {
    myHostRegion.setExpanded(expanded);
  }

  @Override
  @NotNull
  public String getPlaceholderText() {
    return myHostRegion.getPlaceholderText();
  }

  @Override
  public Editor getEditor() {
    return myEditorWindow;
  }

  @Override
  public FoldingGroup getGroup() {
    return myHostRegion.getGroup();
  }

  @Override
  public boolean shouldNeverExpand() {
    return false;
  }

  @Override
  public FoldRegionImpl getDelegate() {
    return (FoldRegionImpl)myHostRegion;
  }

  @Override
  public void setGutterMarkEnabledForSingleLine(boolean value) {
    myHostRegion.setGutterMarkEnabledForSingleLine(value);
  }

  @Override
  public boolean isGutterMarkEnabledForSingleLine() {
    return myHostRegion.isGutterMarkEnabledForSingleLine();
  }

  @Override
  public void setPlaceholderText(@NotNull String text) {
    myHostRegion.setPlaceholderText(text);
  }
}
