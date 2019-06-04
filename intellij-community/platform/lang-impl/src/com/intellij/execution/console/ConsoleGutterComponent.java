// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.codeInsight.hint.TooltipController;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.ui.HintHint;
import com.intellij.ui.JBColor;
import com.intellij.ui.JreHiDpiUtil;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

class ConsoleGutterComponent extends JComponent implements MouseMotionListener {
  private static final TooltipGroup TOOLTIP_GROUP = new TooltipGroup("CONSOLE_GUTTER_TOOLTIP_GROUP", 0);

  private final EditorImpl editor;

  private int maxContentWidth;
  private int myLastPreferredHeight = -1;
  private final int gap;

  private final GutterContentProvider gutterContentProvider;

  private int lastGutterToolTipLine = -1;

  private final boolean atLineStart;

  ConsoleGutterComponent(@NotNull Editor editor, @NotNull GutterContentProvider gutterContentProvider, boolean atLineStart) {
    this.editor = (EditorImpl)editor;
    this.gutterContentProvider = gutterContentProvider;
    this.atLineStart = atLineStart;

    if (atLineStart) {
      setOpaque(gutterContentProvider.getLineStartGutterOverlap(editor) == 0);
    }
    else {
      addListeners();
      setOpaque(false);
    }

    int spaceWidth = EditorUtil.getSpaceWidth(Font.PLAIN, editor);
    // at line start: icon/one-char symbol + space
    gap = atLineStart ? spaceWidth * GutterContentProvider.MAX_LINE_END_GUTTER_WIDTH_IN_CHAR : spaceWidth;
    maxContentWidth = atLineStart ? gap : 0;
  }

  private void addListeners() {
    addMouseMotionListener(this);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (!e.isPopupTrigger()) {
          gutterContentProvider.doAction(EditorUtil.yPositionToLogicalLine(editor, e.getPoint()), editor);
        }
      }
    });
  }

  public void updateSize(int start, int end) {
    int oldAnnotationsWidth = maxContentWidth;
    computeMaxAnnotationWidth(start, end);
    if (oldAnnotationsWidth != maxContentWidth || myLastPreferredHeight != editor.getPreferredHeight()) {
      processComponentEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
    }
    repaint();
  }

  private void computeMaxAnnotationWidth(int start, int end) {
    gutterContentProvider.beforeUiComponentUpdate(editor);

    if (atLineStart) {
      return;
    }

    if (!gutterContentProvider.hasText()) {
      editor.getSettings().setAdditionalColumnsCount(1);
      maxContentWidth = 0;
      return;
    }

    FontMetrics fontMetrics = editor.getFontMetrics(Font.PLAIN);
    int lineCount = Math.min(end, editor.getDocument().getLineCount());
    int gutterSize = 0;
    for (int line = start; line < lineCount; line++) {
      String text = gutterContentProvider.getText(line, editor);
      if (text != null) {
        gutterSize = Math.max(gutterSize, fontMetrics.stringWidth(text));
      }
    }

    // line start gutter always has gap
    if (gutterSize != 0) {
      gutterSize += gap;
    }
    maxContentWidth = Math.max(gutterSize, maxContentWidth);

    editor.getSettings().setAdditionalColumnsCount(1 + (maxContentWidth / EditorUtil.getSpaceWidth(Font.PLAIN, editor)));
  }

  @Override
  public Dimension getPreferredSize() {
    myLastPreferredHeight = editor.getPreferredHeight();
    return new Dimension(maxContentWidth, myLastPreferredHeight);
  }

  public int getPreferredWidth() {
    return maxContentWidth;
  }

  @Override
  public void paint(Graphics g) {
    Rectangle clip = g.getClipBounds();
    if (clip.height <= 0 || maxContentWidth == 0) {
      return;
    }

    if (atLineStart) {
      // don't paint in the overlapped region
      if (clip.x >= maxContentWidth) {
        return;
      }

      g.setColor(editor.getBackgroundColor());
      g.fillRect(clip.x, clip.y, Math.min(clip.width, maxContentWidth - clip.x), clip.height);
    }

    UISettings.setupAntialiasing(g);

    Graphics2D g2 = (Graphics2D)g;
    Object hint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    if (!JreHiDpiUtil.isJreHiDPI(g2)) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    try {
      paintAnnotations(g, clip);
    }
    finally {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
    }
  }

  private void paintAnnotations(Graphics g, Rectangle clip) {
    int lineHeight = editor.getLineHeight();
    int startLine = clip.y / lineHeight;
    int endLine = Math.min(((clip.y + clip.height) / lineHeight) + 1, editor.getVisibleLineCount());
    if (startLine >= endLine) {
      return;
    }

    if (!atLineStart) {
      g.setColor(JBColor.BLUE);
    }
    g.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));

    int y = ((startLine + 1) * lineHeight) - editor.getDescent();
    FontMetrics fontMetrics = editor.getFontMetrics(Font.PLAIN);
    for (int line = startLine; line < endLine; line++) {
      int logicalLine = editor.visualToLogicalPosition(new VisualPosition(line, 0)).line;
      if (atLineStart) {
        gutterContentProvider.drawIcon(logicalLine, g, y, editor);
      }
      else {
        String text = gutterContentProvider.getText(logicalLine, editor);
        if (text != null) {
          // right-aligned
          g.drawString(text, maxContentWidth - gap - fontMetrics.stringWidth(text), y);
        }
      }
      y += lineHeight;
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    TooltipController.getInstance().cancelTooltips();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    int line = EditorUtil.yPositionToLogicalLine(editor, e.getPoint());
    if (line == lastGutterToolTipLine) {
      return;
    }

    TooltipController controller = TooltipController.getInstance();
    if (lastGutterToolTipLine != -1) {
      controller.cancelTooltip(TOOLTIP_GROUP, e, true);
    }

    String toolTip = gutterContentProvider.getToolTip(line, editor);
    setCursor(toolTip == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    if (toolTip == null) {
      lastGutterToolTipLine = -1;
      controller.cancelTooltip(TOOLTIP_GROUP, e, false);
    }
    else {
      lastGutterToolTipLine = line;
      RelativePoint showPoint = new RelativePoint(this, e.getPoint());
      controller.showTooltipByMouseMove(editor,
                                        showPoint,
                                        ((EditorMarkupModel)editor.getMarkupModel()).getErrorStripTooltipRendererProvider().calcTooltipRenderer(toolTip),
                                        false,
                                        TOOLTIP_GROUP,
                                        new HintHint(this, e.getPoint()).setAwtTooltip(true));
    }
  }

  public void documentCleared() {
    if (!atLineStart) {
      maxContentWidth = 0;
    }
  }
}