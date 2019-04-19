// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.HintHint;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.VerticalBox;
import com.intellij.ui.components.panels.Wrapper;
import java.util.HashMap;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

class TrafficProgressPanel extends JPanel {
  static final String MAX_TEXT = "100%";
  private static final String MIN_TEXT = "0%";

  private final JLabel statistics = new JLabel();
  private final Map<JProgressBar, JLabel> myProgressToText = new HashMap<>();

  private final JLabel statusLabel = new JLabel();
  private final JLabel statusExtraLineLabel = new JLabel();
  @NotNull private final TrafficLightRenderer myTrafficLightRenderer;

  private final JPanel myPassStatuses = new JPanel();
  private final JPanel myEmptyPassStatuses = new NonOpaquePanel();
  private final Wrapper myPassStatusesContainer = new Wrapper();

  @NotNull private final HintHint myHintHint;

  TrafficProgressPanel(@NotNull TrafficLightRenderer trafficLightRenderer, @NotNull Editor editor, @NotNull HintHint hintHint) {
    myHintHint = hintHint;
    myTrafficLightRenderer = trafficLightRenderer;

    setLayout(new BorderLayout());

    VerticalBox center = new VerticalBox();

    add(center, BorderLayout.NORTH);
    center.add(statusLabel);
    center.add(statusExtraLineLabel);
    center.add(new Separator());
    center.add(Box.createVerticalStrut(6));

    TrafficLightRenderer.DaemonCodeAnalyzerStatus fakeStatusLargeEnough = new TrafficLightRenderer.DaemonCodeAnalyzerStatus();
    fakeStatusLargeEnough.errorCount = new int[]{1, 1, 1, 1};
    Project project = trafficLightRenderer.getProject();
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    fakeStatusLargeEnough.passStati = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      fakeStatusLargeEnough.passStati
        .add(new ProgressableTextEditorHighlightingPass(project, null, DaemonBundle.message("pass.wolf"), psiFile, editor, TextRange.EMPTY_RANGE, false,
                                                        HighlightInfoProcessor.getEmpty()) {
          @Override
          protected void collectInformationWithProgress(@NotNull ProgressIndicator progress) {
          }

          @Override
          protected void applyInformationWithProgress() {
          }
        });
    }
    center.add(myPassStatusesContainer);

    add(statistics, BorderLayout.SOUTH);
    updatePanel(fakeStatusLargeEnough, true);

    hintHint.initStyle(this, true);
    statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
  }

  int getMinWidth() {
    return Math.max(Math.max(Math.max(getLabelMinWidth(statistics), getLabelMinWidth(statusExtraLineLabel)), getLabelMinWidth(statusLabel)), getLabelMinWidth(new JLabel("<html><b>Slow inspections progress report long line</b></html>")));
  }

  private int getLabelMinWidth(@NotNull JLabel label) {
    String text = label.getText();
    Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

    if (icon == null && StringUtil.isEmpty(text)) {
      return 0;
    }

    Rectangle paintIconR = new Rectangle();
    Rectangle paintTextR = new Rectangle();
    Rectangle paintViewR = new Rectangle(10000, 10000);

    SwingUtilities.layoutCompoundLabel(
      label,
      getFontMetrics(getFont()),
      text,
      icon,
      label.getVerticalAlignment(),
      label.getHorizontalAlignment(),
      label.getVerticalTextPosition(),
      label.getHorizontalTextPosition(),
      paintViewR,
      paintIconR,
      paintTextR,
      label.getIconTextGap());

    return paintTextR.width;
  }

  private class Separator extends NonOpaquePanel {
    @Override
    protected void paintComponent(@NotNull Graphics g) {
      Insets insets = getInsets();
      if (insets == null) {
        insets = new Insets(0, 0, 0, 0);
      }
      g.setColor(myHintHint.getTextForeground());
      g.drawLine(insets.left, insets.top, getWidth() - insets.left - insets.right, insets.top);
    }

    @NotNull
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(1, 1);
    }

    @NotNull
    @Override
    public Dimension getMinimumSize() {
      return new Dimension(1, 1);
    }
  }

  void updatePanel(@NotNull TrafficLightRenderer.DaemonCodeAnalyzerStatus status, boolean isFake) {
    try {
      boolean needRebuild = myTrafficLightRenderer.updatePanel(status);
      statusLabel.setText(myTrafficLightRenderer.statusLabel);
      if (myTrafficLightRenderer.statusExtraLine == null) {
        statusExtraLineLabel.setVisible(false);
      }
      else {
        statusExtraLineLabel.setText(myTrafficLightRenderer.statusExtraLine);
        statusExtraLineLabel.setVisible(true);
      }
      myPassStatuses.setVisible(myTrafficLightRenderer.passStatusesVisible);
      statistics.setText(myTrafficLightRenderer.statistics);
      resetProgressBars(myTrafficLightRenderer.progressBarsEnabled, myTrafficLightRenderer.progressBarsCompleted);

      if (needRebuild) {
        // passes set has changed
        rebuildPassesProgress(status);
      }

      for (ProgressableTextEditorHighlightingPass pass : status.passStati) {
        double progress = pass.getProgress();
        Pair<JProgressBar, JLabel> pair = myTrafficLightRenderer.passes.get(pass);
        JProgressBar progressBar = pair.first;
        int percent = (int)Math.round(progress * TrafficLightRenderer.MAX);
        if (percent == 100 && !pass.isFinished()) {
          percent = 99;
        }
        progressBar.setValue(percent);
        JLabel percentage = pair.second;
        percentage.setText(percent + "%");
      }
    }
    finally {
      if (isFake) {
        myEmptyPassStatuses.setPreferredSize(myPassStatuses.getPreferredSize());
        myPassStatusesContainer.setContent(myEmptyPassStatuses);
      }
      else {
        myPassStatusesContainer.setContent(myPassStatuses);
      }
    }
  }

  private void resetProgressBars(final boolean enabled, @Nullable final Boolean completed) {
    for (JProgressBar progress : UIUtil.uiTraverser(myPassStatuses).traverse().filter(JProgressBar.class)) {
      progress.setEnabled(enabled);
      if (completed != null) {
        if (completed) {
          progress.setValue(TrafficLightRenderer.MAX);
          myProgressToText.get(progress).setText(MAX_TEXT);
        }
        else {
          progress.setValue(0);
          myProgressToText.get(progress).setText(MIN_TEXT);
        }
      }
    }
  }

  private void rebuildPassesProgress(@NotNull TrafficLightRenderer.DaemonCodeAnalyzerStatus status) {
    myPassStatuses.removeAll();
    myPassStatuses.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    for (ProgressableTextEditorHighlightingPass pass : status.passStati) {
      JLabel label = new JLabel(pass.getPresentableName() + ": ");
      label.setHorizontalTextPosition(SwingConstants.RIGHT);

      Pair<JProgressBar, JLabel> pair = myTrafficLightRenderer.passes.get(pass);
      JProgressBar progressBar = pair.getFirst();
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, progressBar);
      JLabel percLabel = pair.getSecond();
      myProgressToText.put(progressBar, percLabel);
      c.gridx = 0;
      myPassStatuses.add(label, c);
      c.gridx = 1;
      myPassStatuses.add(progressBar, c);
      c.gridx = 2;
      c.weightx = 1;
      myPassStatuses.add(percLabel, c);

      c.gridy++;
    }

    myHintHint.initStyle(myPassStatuses, true);
  }
}
