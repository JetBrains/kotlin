// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.internal.statistic.service.fus.collectors.UIEventId;
import com.intellij.internal.statistic.service.fus.collectors.UIEventLogger;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.fileEditor.impl.EditorWindowHolder;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.intellij.codeInsight.daemon.impl.ConfigureHighlightingLevelKt.getConfigureHighlightingLevelPopup;

public class DaemonEditorPopup extends PopupHandler {
  private final Project myProject;
  private final Editor myEditor;

  DaemonEditorPopup(@NotNull final Project project, @NotNull final Editor editor) {
    myProject = project;
    myEditor = editor;
  }

  @Override
  public void invokePopup(final Component comp, final int x, final int y) {
    if (ApplicationManager.getApplication() == null) return;
    final PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (file == null) return;

    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    DefaultActionGroup gotoGroup = createGotoGroup();
    actionGroup.add(gotoGroup);
    actionGroup.addSeparator();
    actionGroup.add(new AnAction(EditorBundle.messagePointer("customize.highlighting.level.menu.item")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        JBPopup popup = getConfigureHighlightingLevelPopup(e.getDataContext());
        if (popup != null) popup.show(new RelativePoint(comp, new Point(x, y)));
      }
    });
    if (!UIUtil.uiParents(myEditor.getComponent(), false).filter(EditorWindowHolder.class).isEmpty()) {
      actionGroup.addSeparator();
      actionGroup.add(new ToggleAction(IdeBundle.message("checkbox.show.editor.preview.popup")) {
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
          return UISettings.getInstance().getShowEditorToolTip();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
          UISettings.getInstance().setShowEditorToolTip(state);
          UISettings.getInstance().fireUISettingsChanged();
        }
      });
    }
    ActionPopupMenu editorPopup = actionManager.createActionPopupMenu(ActionPlaces.RIGHT_EDITOR_GUTTER_POPUP, actionGroup);
    if (DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(file)) {
      UIEventLogger.logUIEvent(UIEventId.DaemonEditorPopupInvoked);
      editorPopup.getComponent().show(comp, x, y);
    }
  }

  @NotNull
  static DefaultActionGroup createGotoGroup() {
    Shortcut shortcut = KeymapUtil.getPrimaryShortcut("GotoNextError");
    String shortcutText = shortcut != null ? " (" + KeymapUtil.getShortcutText(shortcut) + ")" : "";
    DefaultActionGroup gotoGroup = DefaultActionGroup.createPopupGroup(() -> CodeInsightBundle.message("popup.title.next.error.action.0.goes.through", shortcutText));
    gotoGroup.add(new ToggleAction(EditorBundle.message("errors.panel.go.to.errors.first.radio")) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                      return DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                      DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(state);
                    }

                    @Override
                    public boolean isDumbAware() {
                      return true;
                    }
                  }
    );
    gotoGroup.add(new ToggleAction(EditorBundle.message("errors.panel.go.to.next.error.warning.radio")) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                      return !DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                      DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(!state);
                    }

                    @Override
                    public boolean isDumbAware() {
                      return true;
                    }
                  }
    );
    return gotoGroup;
  }
}