// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.internal.statistic.service.fus.collectors.UIEventId;
import com.intellij.internal.statistic.service.fus.collectors.UIEventLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.ui.JBCheckboxMenuItem;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.psi.PsiFile;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.GraphicsUtil;

import javax.swing.*;
import java.awt.*;

public class DaemonEditorPopup extends PopupHandler {
  private final PsiFile myPsiFile;

  DaemonEditorPopup(final PsiFile psiFile) {
    myPsiFile = psiFile;
  }

  @Override
  public void invokePopup(final Component comp, final int x, final int y) {
    if (ApplicationManager.getApplication() == null) return;
    final JRadioButtonMenuItem errorsFirst = createRadioButtonMenuItem(EditorBundle.message("errors.panel.go.to.errors.first.radio"));
    errorsFirst.addActionListener(
      __ -> DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(errorsFirst.isSelected()));
    final JPopupMenu popupMenu = new JBPopupMenu();
    popupMenu.add(errorsFirst);

    final JRadioButtonMenuItem next = createRadioButtonMenuItem(EditorBundle.message("errors.panel.go.to.next.error.warning.radio"));
    next.addActionListener(__ -> DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(!next.isSelected()));
    popupMenu.add(next);

    ButtonGroup group = new ButtonGroup();
    group.add(errorsFirst);
    group.add(next);

    popupMenu.addSeparator();
    final JMenuItem hLevel = new JBMenuItem(EditorBundle.message("customize.highlighting.level.menu.item"));
    popupMenu.add(hLevel);

    final boolean isErrorsFirst = DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
    errorsFirst.setSelected(isErrorsFirst);
    next.setSelected(!isErrorsFirst);
    hLevel.addActionListener(__ -> {
      final PsiFile psiFile = myPsiFile;
      if (psiFile == null) return;
      final HectorComponent component = new HectorComponent(psiFile);
      final Dimension dimension = component.getPreferredSize();
      Point point = new Point(x, y);
      component.showComponent(new RelativePoint(comp, new Point(point.x - dimension.width, point.y)));
    });

    final JBCheckboxMenuItem previewCheckbox = new JBCheckboxMenuItem(IdeBundle.message("checkbox.show.editor.preview.popup"), UISettings.getInstance().getShowEditorToolTip());
    popupMenu.addSeparator();
    popupMenu.add(previewCheckbox);
    previewCheckbox.addActionListener(__ -> {
      UISettings.getInstance().setShowEditorToolTip(previewCheckbox.isSelected());
      UISettings.getInstance().fireUISettingsChanged();
    });

    PsiFile file = myPsiFile;
    if (file != null && DaemonCodeAnalyzer.getInstance(myPsiFile.getProject()).isHighlightingAvailable(file)) {
      UIEventLogger.logUIEvent(UIEventId.DaemonEditorPopupInvoked);
      popupMenu.show(comp, x, y);
    }
  }

  private static JRadioButtonMenuItem createRadioButtonMenuItem(final String message) {
    return new JRadioButtonMenuItem(message) {
      @Override
      public void paint(Graphics g) {
        GraphicsUtil.setupAntialiasing(g);
        super.paint(g);
      }
    };
  }
}