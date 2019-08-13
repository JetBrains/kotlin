// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.LineTooltipRenderer;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.impl.TrafficTooltipRenderer;
import com.intellij.ui.HintHint;
import com.intellij.ui.HintListener;
import com.intellij.ui.LightweightHint;
import com.intellij.util.ui.update.ComparableObject;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.EventObject;

class TrafficTooltipRendererImpl extends ComparableObject.Impl implements TrafficTooltipRenderer {
  private TrafficProgressPanel myPanel;
  private final Runnable onHide;
  private TrafficLightRenderer myTrafficLightRenderer;

  TrafficTooltipRendererImpl(@NotNull Runnable onHide, @NotNull Editor editor) {
    super(editor);
    this.onHide = onHide;
  }

  @Override
  public void repaintTooltipWindow() {
    if (myPanel != null) {
      SeverityRegistrar severityRegistrar = myTrafficLightRenderer.getSeverityRegistrar();
      TrafficLightRenderer.DaemonCodeAnalyzerStatus status = myTrafficLightRenderer.getDaemonCodeAnalyzerStatus(severityRegistrar);
      myPanel.updatePanel(status, false);
    }
  }

  @Override
  public LightweightHint show(@NotNull Editor editor, @NotNull Point p, boolean alignToRight, @NotNull TooltipGroup group, @NotNull HintHint hintHint) {
    myTrafficLightRenderer = (TrafficLightRenderer)((EditorMarkupModelImpl)editor.getMarkupModel()).getErrorStripeRenderer();
    myPanel = new TrafficProgressPanel(myTrafficLightRenderer, editor, hintHint);
    repaintTooltipWindow();
    LineTooltipRenderer.correctLocation(editor, myPanel, p, alignToRight, true, myPanel.getMinWidth());
    LightweightHint hint = new LightweightHint(myPanel);

    HintManagerImpl hintManager = (HintManagerImpl)HintManager.getInstance();
    hintManager.showEditorHint(hint, editor, p,
                               HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_OTHER_HINT |
                               HintManager.HIDE_BY_SCROLLING, 0, false, hintHint);
    hint.addHintListener(new HintListener() {
      @Override
      public void hintHidden(@NotNull EventObject event) {
        if (myPanel == null) return; //double hide?
        myPanel = null;
        onHide.run();
      }
    });
    return hint;
  }
}
