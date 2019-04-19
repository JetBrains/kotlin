/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import com.intellij.codeInsight.hint.TooltipController;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.codeInsight.hint.TooltipRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.ex.ErrorStripTooltipRendererProvider;
import com.intellij.openapi.editor.ex.TooltipAction;
import com.intellij.ui.HintHint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author max
 */
public class DaemonTooltipUtil {
  private static final TooltipGroup DAEMON_INFO_GROUP = new TooltipGroup("DAEMON_INFO_GROUP", 0);

  public static void showInfoTooltip(HighlightInfo info, Editor editor, int defaultOffset) {
    showInfoTooltip(info, editor, defaultOffset, -1);
  }

  public static void cancelTooltips() {
    TooltipController.getInstance().cancelTooltip(DAEMON_INFO_GROUP, null, true);
  }

  private static void showInfoTooltip(@NotNull final HighlightInfo info,
                                      @NotNull Editor editor,
                                      final int defaultOffset,
                                      final int currentWidth) {
    showInfoTooltip(info, editor, defaultOffset, currentWidth, false);
  }
  

  static void showInfoTooltip(@NotNull final HighlightInfo info,
                              @NotNull Editor editor,
                              final int defaultOffset,
                              final int currentWidth,
                              final boolean requestFocus) {
    String text = info.getToolTip();
    if (text == null) return;
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();

    Point point = editor.logicalPositionToXY(editor.offsetToLogicalPosition(defaultOffset));
    Point highlightEndPoint = editor.logicalPositionToXY(editor.offsetToLogicalPosition(info.endOffset));
    if (highlightEndPoint.y > point.y) {
      if (highlightEndPoint.x > point.x) {
        point = new Point(point.x, highlightEndPoint.y);
      } else if (highlightEndPoint.y > point.y + editor.getLineHeight()) {
        point = new Point(point.x, highlightEndPoint.y - editor.getLineHeight());
      }
    }
    
    Point bestPoint = new Point(point);
    bestPoint.y += editor.getLineHeight() / 2;
    if (!visibleArea.contains(bestPoint)) bestPoint = point;

    Point p = SwingUtilities.convertPoint(
      editor.getContentComponent(),
      bestPoint,
      editor.getComponent().getRootPane().getLayeredPane()
    );

    HintHint hintHint = new HintHint(editor, bestPoint)
      .setAwtTooltip(true)
      .setHighlighterType(true)
      .setRequestFocus(requestFocus)
      .setCalloutShift(editor.getLineHeight() / 2 - 1);


    TooltipAction action = TooltipActionProvider.calcTooltipAction(info, editor);
    ErrorStripTooltipRendererProvider provider = ((EditorMarkupModel)editor.getMarkupModel()).getErrorStripTooltipRendererProvider();
    TooltipRenderer tooltipRenderer = provider.calcTooltipRenderer(text, action, currentWidth);
    
    TooltipController.getInstance().showTooltip(editor, p, tooltipRenderer, false, DAEMON_INFO_GROUP, hintHint);
  }
}
